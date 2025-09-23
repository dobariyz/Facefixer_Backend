import sys
import cv2
import numpy as np
import json   #added
from ultralytics import YOLO

# Get command-line arguments
model_path = sys.argv[1]  # YOLO model file
image_path = sys.argv[2]  # Input image file
output_path = sys.argv[3]  # Output image file
json_output_path = sys.argv[4]  # <-- NEW: path to save JSON summary

# Load model
model = YOLO(model_path)

# Read image
frame = cv2.imread(image_path)

# Run inference
results = model(frame, verbose=False)
detections = results[0].boxes

# Define colors
bbox_colors = [(164,120,87), (68,148,228), (93,97,209), (178,182,133)]

# Container for summary
# summary = {}

# JSON container (new structured format)
json_result = {
    "summary": {},        # aggregated stats
    "detections": [],     # raw detections with bbox + conf
    "recommendations": [] # future use
}


# Draw detections
for i in range(len(detections)):
    xyxy = detections[i].xyxy.cpu().numpy().squeeze().astype(int)
    xmin, ymin, xmax, ymax = xyxy
    classidx = int(detections[i].cls.item())
    conf = detections[i].conf.item()
    
    if conf > 0.2:
        color = bbox_colors[classidx % len(bbox_colors)]
        cv2.rectangle(frame, (xmin, ymin), (xmax, ymax), color, 2)
        label = f'{model.names[classidx]}: {int(conf*100)}%'
        cv2.putText(frame, label, (xmin, ymin - 5), cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 2)
        
        # Detection entry
        class_name = model.names[classidx]
        detection_entry = {
            "class": class_name,
            "confidence": round(conf * 100, 2),
            "bbox": [int(xmin), int(ymin), int(xmax), int(ymax)]
        }
        json_result["detections"].append(detection_entry)

        # Update summary stats
        if class_name not in json_result["summary"]:
            json_result["summary"][class_name] = {
                "count": 0,
                "confidences": []
            }
        json_result["summary"][class_name]["count"] += 1
        json_result["summary"][class_name]["confidences"].append(detection_entry["confidence"])

# Compute min/max/avg for summary
for cls, stats in json_result["summary"].items():
    confs = stats["confidences"]
    stats["min_conf"] = min(confs)
    stats["max_conf"] = max(confs)
    stats["avg_conf"] = round(sum(confs) / len(confs), 2)
    del stats["confidences"]  # remove raw list to keep JSON clean
    
# Save the image
cv2.imwrite(output_path, frame)

# Save summary JSON
with open(json_output_path, "w") as f:
    json.dump(json_result, f, indent=4)

# Print paths so Java can read
print(f"IMAGE_OUTPUT={output_path}")
print(f"JSON_OUTPUT={json_output_path}")