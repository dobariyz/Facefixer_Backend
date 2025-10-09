package ca.sheridancollege.dobariyz.services;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ca.sheridancollege.dobariyz.beans.DetectionResult;
import ca.sheridancollege.dobariyz.beans.User;
import ca.sheridancollege.dobariyz.repositories.DetectionResultRepository;
import ca.sheridancollege.dobariyz.repositories.UserRepository;
import ca.sheridancollege.dobariyz.repositories.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DetectionResultService {

    private final DetectionResultRepository repository;
    private final UserRepository userRepository; // ✅ add this

    public DetectionResult saveResult(DetectionResult result) {
        return repository.save(result);
    }

    public List<DetectionResult> getResultsByUser(User user) {
        return repository.findByUser(user);
    }

    public Optional<DetectionResult> findLatestByUserEmail(String email) {
        Optional<User> userOpt = userRepository.findFirstByEmail(email);
        return userOpt.flatMap(user -> repository.findFirstByUserOrderByCreatedAtDesc(user));
    }
    
    public Optional<DetectionResult> findById(Long id) {
        return repository.findById(id);
    }


}
