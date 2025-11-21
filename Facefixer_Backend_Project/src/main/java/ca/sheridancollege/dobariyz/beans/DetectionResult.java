package ca.sheridancollege.dobariyz.beans;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "detection_results")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DetectionResult {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link to logged-in user
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    private String imagePath;   // original uploaded
    private String resultPath;  // YOLO output

    @Column(columnDefinition = "TEXT")
    private String detections;  // JSON string

    private LocalDateTime createdAt = LocalDateTime.now();
}
