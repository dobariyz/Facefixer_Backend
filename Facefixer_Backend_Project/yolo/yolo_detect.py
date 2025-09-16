import sys
import cv2
import numpy as np
from ultralytics import YOLO

# Get command-line arguments
model_path = sys.argv[1]  # YOLO model file
image_path = sys.argv[2]  # Input image file
output_path = sys.argv[3]  # Output image file

# Load model
model = YOLO(model_path)

# Read image
frame = cv2.imread(image_path)

# Run inference
results = model(frame, verbose=False)
detections = results[0].boxes

# Define colors
bbox_colors = [(164,120,87), (68,148,228), (93,97,209), (178,182,133)]

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

# Save the image
cv2.imwrite(output_path, frame)
print(output_path)  # Print output path so Java can read it
