	package ca.sheridancollege.dobariyz.controllers;
	
	import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ca.sheridancollege.dobariyz.beans.DetectionResult;
import ca.sheridancollege.dobariyz.beans.Subscription;
import ca.sheridancollege.dobariyz.beans.User;
import ca.sheridancollege.dobariyz.repositories.SubscriptionRepository;
import ca.sheridancollege.dobariyz.repositories.UserRepository;
import ca.sheridancollege.dobariyz.services.DetectionResultService;
import ca.sheridancollege.dobariyz.services.S3Service;
//import ca.sheridancollege.dobariyz.services.S3Service;
import ca.sheridancollege.dobariyz.util.JwtUtil;

//	@CrossOrigin(origins = "http://localhost:5173")
//	@RestController
//	@RequestMapping("/api")
//	public class YoloController {
//		
//		@Autowired
//		private JwtUtil jwtService;
//
//		@Autowired
//		private UserRepository userRepository;
//		
//		//AWS
//		@Autowired
//		private S3Service s3Service;
//		
//		@Autowired
//		private DetectionResultService detectionResultService;
//		
//		private static final String YOLO_SCRIPT = "yolo/yolo_detect.py";
//		private static final String MODEL_PATH = "yolo/my_model.pt";
//	    private static final String UPLOAD_DIR = "uploads/";
//	    private static final String OUTPUT_DIR = "outputs/";
//	
//	    @CrossOrigin(origins = "http://localhost:5173")
//	    @PostMapping("/detect")
//	    public ResponseEntity<Map<String, String>> detectObjects(
//	            Authentication authentication,
//	            @RequestParam("file") MultipartFile file) {
//	        try {
//	            if (authentication == null || !authentication.isAuthenticated()) {
//	                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//	                        .body(Map.of("error", "Unauthorized"));
//	            }
//
//	            String email = null;
//	            if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
//	                email = oauth2User.getAttribute("email");  // ✅ This is the actual Google email
//	            } else if (authentication.getPrincipal() instanceof UserDetails userDetails) {
//	                email = userDetails.getUsername();
//	            }
//
//	            if (email == null) {
//	                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//	                        .body(Map.of("error", "Email not found in authentication."));
//	            }
//
//	            // Save uploaded image
//	            Path imagePath = Paths.get(UPLOAD_DIR + file.getOriginalFilename());
//	            Files.createDirectories(imagePath.getParent());
//	            Files.write(imagePath, file.getBytes());
//
//	            // Output image path
//	            String outputImagePath = OUTPUT_DIR + "detected_" + file.getOriginalFilename();
//	            Files.createDirectories(Paths.get(OUTPUT_DIR));
//
//	            // Run YOLO script
//	            String jsonOutputPath = OUTPUT_DIR + "summary_" + file.getOriginalFilename() + ".json";
//
//	            ProcessBuilder pb = new ProcessBuilder(
//	                    "C:\\Users\\user\\AppData\\Local\\Programs\\Python\\Python310\\python.exe",
//	                    YOLO_SCRIPT,
//	                    MODEL_PATH,
//	                    imagePath.toString(),
//	                    outputImagePath,
//	                    jsonOutputPath
//	                    
//	            );
//	            pb.redirectErrorStream(true);
//	            Process process = pb.start();
//
//	            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//		         String line;
//		         while ((line = reader.readLine()) != null) {
//		             System.out.println("PYTHON >> " + line);
//		         }
//		         process.waitFor();
//
//		         // Read JSON file content (after Python finishes)
//		         String detectionsJson = "";
//		         Path jsonPath = Paths.get(jsonOutputPath);
//		         if (Files.exists(jsonPath)) {
//		             detectionsJson = Files.readString(jsonPath);  // <-- load JSON string
//		         } else {
//		             detectionsJson = "{}"; // fallback
//		         }
//
//	            // Fetch user by email
//	            Optional<User> userOpt = userRepository.findFirstByEmail(email);
//	            if (userOpt.isEmpty()) {
//	                System.out.println("❌ No user found for email: " + email);
//	                return ResponseEntity.status(HttpStatus.NOT_FOUND)
//	                        .body(Map.of("error", "User not found: " + email));
//	            }
//
//	            User user = userOpt.get();
//
//	            // Save detection result
//	            DetectionResult result = new DetectionResult();
//	            result.setUser(user);
//	            result.setImagePath(file.getOriginalFilename());
//	            result.setResultPath("detected_" + file.getOriginalFilename());
//	            result.setCreatedAt(LocalDateTime.now());
//	            result.setDetections(detectionsJson);  //  now saving actual JSON!
//
//
//	            detectionResultService.saveResult(result);
//
//	            return ResponseEntity.ok(Map.of(
//	                "uploaded", "/api/image?file=" + file.getOriginalFilename(),
//	                "processed", "/api/image?file=" + "detected_" + file.getOriginalFilename(),
//	                "detections", result.getDetections() // <-- JSON string
//	            ));
//
//	        } catch (Exception e) {
//	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//	                    .body(Map.of("error", "Error processing image: " + e.getMessage()));
//	        }
//	    }

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api")
public class YoloController {
    
    @Autowired
    private JwtUtil jwtService;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private S3Service s3Service;
    
    @Autowired
    private DetectionResultService detectionResultService;
    
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    
    private static final String YOLO_SCRIPT = "yolo/yolo_detect.py";
    private static final String MODEL_PATH = "yolo/my_model.pt";

    @CrossOrigin(origins = "http://localhost:5173")
    @PostMapping("/detect")
    @Transactional // ✅ Ensure atomic updates
    public ResponseEntity<Map<String, Object>> detectObjects(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {

        Path tempDir = null;
        Path originalPath = null;
        Path processedPath = null;
        Path jsonPath = null;

        try {
            // ✅ 1. Authentication Check
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Unauthorized"));
            }

            // ✅ 2. Get User Email
            String email = null;
            if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
                email = oauth2User.getAttribute("email");
            } else if (authentication.getPrincipal() instanceof UserDetails userDetails) {
                email = userDetails.getUsername();
            }

            if (email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Email not found in authentication"));
            }

            // ✅ 3. Fetch User from Database
            Optional<User> userOpt = userRepository.findFirstByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found: " + email));
            }
            User user = userOpt.get();

            // ✅ 4. CHECK SUBSCRIPTION LIMITS (NEW!)
            Optional<Subscription> subOpt = subscriptionRepository.findByUserId(user.getId());
            if (subOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                            "error", "no_subscription",
                            "message", "No subscription found. Please contact support."
                        ));
            }

            Subscription subscription = subOpt.get();

            // ✅ Auto-reset if monthly period passed (your entity handles this!)
            // Check if user can upload (also handles expired premium subscriptions)
            if (!subscription.canUpload()) {
                // Save the subscription if reset happened
                subscriptionRepository.save(subscription);
                
                // Check again after potential reset
                if (!subscription.canUpload()) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of(
                                "error", "limit_reached",
                                "message", subscription.getTier().equals("free") 
                                    ? "You've used all 5 free uploads this month. Upgrade to premium for 50 uploads/month or pro for unlimited!"
                                    : "Upload limit reached for your tier. Please upgrade or wait for monthly reset.",
                                "uploadsUsed", subscription.getUploadsUsed(),
                                "uploadsLimit", subscription.getUploadsLimit(),
                                "tier", subscription.getTier(),
                                "resetDate", subscription.getResetDate() != null ? subscription.getResetDate().toString() : null,
                                "needsUpgrade", true
                            ));
                }
            }

            System.out.println("✅ Subscription check passed. Uploads: " + 
                subscription.getUploadsUsed() + "/" + subscription.getUploadsLimit() + 
                " (Tier: " + subscription.getTier() + ")");

            // ✅ 5. Create Temp Directory for Processing
            tempDir = Files.createTempDirectory("facefixer-upload-");
            originalPath = tempDir.resolve(file.getOriginalFilename());
            processedPath = tempDir.resolve("detected_" + file.getOriginalFilename());
            jsonPath = tempDir.resolve("summary_" + file.getOriginalFilename() + ".json");

            // ✅ 6. Save Uploaded File Temporarily
            Files.write(originalPath, file.getBytes());
            System.out.println("📁 Saved temp file: " + originalPath);

            // ✅ 7. Run YOLO Detection Script
            ProcessBuilder pb = new ProcessBuilder(
                    "C:\\Users\\user\\AppData\\Local\\Programs\\Python\\Python310\\python.exe",
                    YOLO_SCRIPT,
                    MODEL_PATH,
                    originalPath.toString(),
                    processedPath.toString(),
                    jsonPath.toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read Python output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("PYTHON >> " + line);
            }
            process.waitFor();
            System.out.println("✅ YOLO detection completed");

            // ✅ 8. Read Detection JSON Results
            String detectionsJson = "{}";
            if (Files.exists(jsonPath)) {
                detectionsJson = Files.readString(jsonPath);
                System.out.println("📊 Detections JSON loaded");
            } else {
                System.out.println("⚠️ No JSON summary file found, using empty object");
            }

            // ✅ 9. Upload Both Images to AWS S3
            String originalS3Key = "uploads/" + file.getOriginalFilename();
            String processedS3Key = "outputs/detected_" + file.getOriginalFilename();

            s3Service.uploadFile(originalS3Key, originalPath);
            System.out.println("☁️ Uploaded original image to S3: " + originalS3Key);

            s3Service.uploadFile(processedS3Key, processedPath);
            System.out.println("☁️ Uploaded processed image to S3: " + processedS3Key);

            // ✅ 10. Save Detection Result to Database
            DetectionResult result = new DetectionResult();
            result.setUser(user);
            result.setImagePath(originalS3Key);
            result.setResultPath(processedS3Key);
            result.setCreatedAt(LocalDateTime.now());
            result.setDetections(detectionsJson);
            detectionResultService.saveResult(result);
            System.out.println("💾 Detection result saved to database");

            // ✅ 11. INCREMENT UPLOAD COUNTER (NEW!)
            subscription.incrementUploads();
            subscriptionRepository.save(subscription);
            System.out.println("📈 Upload counter incremented: " + 
                subscription.getUploadsUsed() + "/" + subscription.getUploadsLimit());

            // ✅ 12. Clean Up Temporary Files
            Files.deleteIfExists(originalPath);
            Files.deleteIfExists(processedPath);
            Files.deleteIfExists(jsonPath);
            Files.deleteIfExists(tempDir);
            System.out.println("🧹 Cleaned up temporary files");

            // ✅ 13. Return Response with Backend Image URLs + Subscription Status
            return ResponseEntity.ok(Map.of(
                    "uploaded", "/api/image?file=" + file.getOriginalFilename(),
                    "processed", "/api/image?file=detected_" + file.getOriginalFilename(),
                    "detections", detectionsJson,
                    "uploadsRemaining", subscription.getRemainingUploads(),
                    "uploadsUsed", subscription.getUploadsUsed(),
                    "uploadsLimit", subscription.getUploadsLimit(),
                    "tier", subscription.getTier(),
                    "resetDate", subscription.getResetDate() != null ? subscription.getResetDate().toString() : null
            ));

        } catch (Exception e) {
            e.printStackTrace();
            
            // Clean up temp files in case of error
            try {
                if (originalPath != null) Files.deleteIfExists(originalPath);
                if (processedPath != null) Files.deleteIfExists(processedPath);
                if (jsonPath != null) Files.deleteIfExists(jsonPath);
                if (tempDir != null) Files.deleteIfExists(tempDir);
            } catch (IOException cleanupEx) {
                System.err.println("⚠️ Failed to cleanup temp files: " + cleanupEx.getMessage());
            }

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error processing image: " + e.getMessage()));
        }
    }

    @CrossOrigin(origins = "http://localhost:5173")
    @GetMapping("/image")
    public ResponseEntity<byte[]> getImage(@RequestParam("file") String filename) {
        try {
            // ✅ Avoid double-prefixing
            String s3Key = filename;
            if (!filename.startsWith("uploads/") && !filename.startsWith("outputs/")) {
                s3Key = filename.startsWith("detected_")
                        ? "outputs/" + filename
                        : "uploads/" + filename;
            }

            System.out.println("📥 Fetching from S3: " + s3Key);

            byte[] fileBytes = s3Service.downloadFile(s3Key);

            String contentType = "image/png";
            if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
                contentType = "image/jpeg";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(fileBytes);

        } catch (Exception e) {
            System.err.println("❌ Error fetching image from S3: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
}