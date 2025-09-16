package ca.sheridancollege.dobariyz.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.sheridancollege.dobariyz.beans.DetectionResult;
import ca.sheridancollege.dobariyz.beans.User;

public interface DetectionResultRepository extends JpaRepository<DetectionResult, Long> {

    List<DetectionResult> findByUser(User user);
    List<DetectionResult> findByUserId(Long userId);


}
