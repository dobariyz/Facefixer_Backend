	package ca.sheridancollege.dobariyz.controllers;
	
	import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ca.sheridancollege.dobariyz.beans.DetectionResult;
import ca.sheridancollege.dobariyz.beans.User;
import ca.sheridancollege.dobariyz.repositories.UserRepository;
import ca.sheridancollege.dobariyz.services.DetectionResultService;
//import ca.sheridancollege.dobariyz.services.S3Service;
import ca.sheridancollege.dobariyz.util.JwtUtil;

	@CrossOrigin(origins = "http://localhost:5173")
	@RestController
	@RequestMapping("/api")
	public class YoloController {
		
		@Autowired
		private JwtUtil jwtService;

		@Autowired
		private UserRepository userRepository;
		
		//AWS
		//@Autowired
		//private S3Service s3Service;
		
		@Autowired
		private DetectionResultService detectionResultService;
		
		private static final String YOLO_SCRIPT = "yolo/yolo_detect.py";
		private static final String MODEL_PATH = "yolo/my_model.pt";
	    private static final String UPLOAD_DIR = "uploads/";
	    private static final String OUTPUT_DIR = "outputs/";
	
	    @CrossOrigin(origins = "http://localhost:5173")
	    @PostMapping("/detect")
	    public ResponseEntity<Map<String, String>> detectObjects(
	            Authentication authentication,
	            @RequestParam("file") MultipartFile file) {
	        try {
	            if (authentication == null || !authentication.isAuthenticated()) {
	                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                        .body(Map.of("error", "Unauthorized"));
	            }

	            String email = null;
	            if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
	                email = oauth2User.getAttribute("email");  // ✅ This is the actual Google email
	            } else if (authentication.getPrincipal() instanceof UserDetails userDetails) {
	                email = userDetails.getUsername();
	            }

	            if (email == null) {
	                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
	                        .body(Map.of("error", "Email not found in authentication."));
	            }

	            // Save uploaded image
	            Path imagePath = Paths.get(UPLOAD_DIR + file.getOriginalFilename());
	            Files.createDirectories(imagePath.getParent());
	            Files.write(imagePath, file.getBytes());

	            // Output image path
	            String outputImagePath = OUTPUT_DIR + "detected_" + file.getOriginalFilename();
	            Files.createDirectories(Paths.get(OUTPUT_DIR));

	            // Run YOLO script
	            String jsonOutputPath = OUTPUT_DIR + "summary_" + file.getOriginalFilename() + ".json";

	            ProcessBuilder pb = new ProcessBuilder(
	                    "C:\\\\Python312\\\\python.exe",
	                    YOLO_SCRIPT,
	                    MODEL_PATH,
	                    imagePath.toString(),
	                    outputImagePath,
	                    jsonOutputPath
	                    
	            );
	            pb.redirectErrorStream(true);
	            Process process = pb.start();

	            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		         String line;
		         while ((line = reader.readLine()) != null) {
		             System.out.println("PYTHON >> " + line);
		         }
		         process.waitFor();

		         // Read JSON file content (after Python finishes)
		         String detectionsJson = "";
		         Path jsonPath = Paths.get(jsonOutputPath);
		         if (Files.exists(jsonPath)) {
		             detectionsJson = Files.readString(jsonPath);  // <-- load JSON string
		         } else {
		             detectionsJson = "{}"; // fallback
		         }

	            // Fetch user by email
	            Optional<User> userOpt = userRepository.findFirstByEmail(email);
	            if (userOpt.isEmpty()) {
	                System.out.println("❌ No user found for email: " + email);
	                return ResponseEntity.status(HttpStatus.NOT_FOUND)
	                        .body(Map.of("error", "User not found: " + email));
	            }

	            User user = userOpt.get();

	            // Save detection result
	            DetectionResult result = new DetectionResult();
	            result.setUser(user);
	            result.setImagePath(file.getOriginalFilename());
	            result.setResultPath("detected_" + file.getOriginalFilename());
	            result.setCreatedAt(LocalDateTime.now());
	            result.setDetections(detectionsJson);  //  now saving actual JSON!


	            detectionResultService.saveResult(result);

	            return ResponseEntity.ok(Map.of(
	                "uploaded", "/api/image?file=" + file.getOriginalFilename(),
	                "processed", "/api/image?file=" + "detected_" + file.getOriginalFilename(),
	                "detections", result.getDetections() // <-- JSON string
	            ));

	        } catch (Exception e) {
	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	                    .body(Map.of("error", "Error processing image: " + e.getMessage()));
	        }
	    }
	    
	 // Only uncomment this if you ready to use AWS, otherwise don't change anything out of it!

//	    @CrossOrigin(origins = "http://localhost:5173")
//	    @PostMapping("/detect")
//	    public ResponseEntity<Map<String, String>> detectObjects(
//	            Authentication authentication,
//	            @RequestParam("file") MultipartFile file) {
//
//	        try {
//	            if (authentication == null || !authentication.isAuthenticated()) {
//	                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//	                        .body(Map.of("error", "Unauthorized"));
//	            }
//
//	            // Get email
//	            String email = null;
//	            if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
//	                email = oauth2User.getAttribute("email");
//	            } else if (authentication.getPrincipal() instanceof UserDetails userDetails) {
//	                email = userDetails.getUsername();
//	            }
//
//	            if (email == null) {
//	                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//	                        .body(Map.of("error", "Email not found"));
//	            }
//
//	            // Temp local storage
//	            Path tempDir = Files.createTempDirectory("facefixer-upload-");
//	            Path originalPath = tempDir.resolve(file.getOriginalFilename());
//	            Files.write(originalPath, file.getBytes());
//
//	            Path processedPath = tempDir.resolve("detected_" + file.getOriginalFilename());
//
//	            // Run YOLO detection
//	            ProcessBuilder pb = new ProcessBuilder(
//	                    "C:\\Users\\user\\AppData\\Local\\Programs\\Python\\Python310\\python.exe",
//	                    YOLO_SCRIPT,
//	                    MODEL_PATH,
//	                    originalPath.toString(),
//	                    processedPath.toString()
//	            );
//	            pb.redirectErrorStream(true);
//	            Process process = pb.start();
//	            process.waitFor();
//
//	            // Upload to S3
//	            s3Service.uploadFile("uploads/" + file.getOriginalFilename(), originalPath);
//	            s3Service.uploadFile("outputs/detected_" + file.getOriginalFilename(), processedPath);
//
//	            // Clean up local temp files
//	            Files.deleteIfExists(originalPath);
//	            Files.deleteIfExists(processedPath);
//	            Files.deleteIfExists(tempDir);
//
//	            // Fetch user
//	            Optional<User> userOpt = userRepository.findFirstByEmail(email);
//	            if (userOpt.isEmpty()) {
//	                return ResponseEntity.status(HttpStatus.NOT_FOUND)
//	                        .body(Map.of("error", "User not found: " + email));
//	            }
//	            User user = userOpt.get();
//
//	            // Save detection result in DB (with S3 keys)
//	            DetectionResult result = new DetectionResult();
//	            result.setUser(user);
//	            result.setImagePath("uploads/" + file.getOriginalFilename());
//	            result.setResultPath("outputs/detected_" + file.getOriginalFilename());
//	            result.setCreatedAt(LocalDateTime.now());
//	            result.setDetections("[]");
//	            detectionResultService.saveResult(result);
//
//	            // ✅ Return backend-accessible URLs for frontend
//	            return ResponseEntity.ok(Map.of(
//	                    "uploaded", "/api/image?file=" + file.getOriginalFilename(),
//	                    "processed", "/api/image?file=" + "detected_" + file.getOriginalFilename()
//	            ));
//
//	        } catch (Exception e) {
//	            e.printStackTrace();
//	            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//	                    .body(Map.of("error", "Error processing image: " + e.getMessage()));
//	        }
//	    }

	    
	    @CrossOrigin(origins = "http://localhost:5173")
	    @GetMapping("/image")
	    public ResponseEntity<Resource> getImage(@RequestParam("file") String filename) throws IOException {
	        Path uploadPath = Paths.get("uploads").resolve(filename);
	        Path outputPath = Paths.get("outputs").resolve(filename);

	        Path filePath = Files.exists(outputPath) ? outputPath : uploadPath;

	        if (!Files.exists(filePath)) {
	            return ResponseEntity.notFound().build();
	        }

	        Resource resource = new UrlResource(filePath.toUri());
	        String contentType = Files.probeContentType(filePath);

	        return ResponseEntity.ok()
	                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
	                .body(resource);
	    }
	}
	    
//	    @CrossOrigin(origins = "http://localhost:5173")
//	    @GetMapping("/image")
//	    public ResponseEntity<byte[]> getImage(@RequestParam("file") String filename) {
//	        try {
//	            String key = filename.startsWith("detected_")
//	                    ? "outputs/" + filename
//	                    : "uploads/" + filename;
//
//	            byte[] fileBytes = s3Service.downloadFile(key);
//
//	            return ResponseEntity.ok()
//	                    .contentType(MediaType.IMAGE_PNG)
//	                    .body(fileBytes);
//	        } catch (Exception e) {
//	            return ResponseEntity.notFound().build();
//	        }
//	    }


	
	
