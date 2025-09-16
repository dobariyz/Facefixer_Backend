package ca.sheridancollege.dobariyz.services;

import java.util.List;

import org.springframework.stereotype.Service;

import ca.sheridancollege.dobariyz.beans.DetectionResult;
import ca.sheridancollege.dobariyz.beans.User;
import ca.sheridancollege.dobariyz.repositories.DetectionResultRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DetectionResultService {

	private final DetectionResultRepository repository;

    public DetectionResult saveResult(DetectionResult result) {
        return repository.save(result);
    }

    public List<DetectionResult> getResultsByUser(User user) {
        return repository.findByUser(user);
    }
}
