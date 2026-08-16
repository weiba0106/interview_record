package com.interviewrecord.interviews.api;

import com.interviewrecord.common.security.CurrentUser;
import com.interviewrecord.common.util.ResourceIds;
import com.interviewrecord.interviews.application.InterviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/interview-rounds")
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class InterviewRoundController {
    private final CurrentUser currentUser;
    private final InterviewService interviewService;

    public InterviewRoundController(CurrentUser currentUser, InterviewService interviewService) {
        this.currentUser = currentUser;
        this.interviewService = interviewService;
    }

    @GetMapping("/questions")
    InterviewDtos.QuestionBankPage searchQuestions(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return interviewService.searchQuestions(currentUser.require().id(), category, keyword, page, size);
    }

    @GetMapping("/questions/random")
    List<InterviewDtos.QuestionBankItem> randomQuestions(
            @RequestParam(defaultValue = "10") int limit) {
        return interviewService.randomQuestions(currentUser.require().id(), limit);
    }

    @GetMapping("/question-categories")
    List<String> questionCategories() {
        return interviewService.questionCategories(currentUser.require().id());
    }

    @GetMapping("/{id}")
    InterviewDtos.RoundResponse get(@PathVariable String id) {
        return interviewService.get(currentUser.require().id(), ResourceIds.parse(id));
    }

    @PutMapping("/{id}")
    InterviewDtos.RoundResponse update(@PathVariable String id,
            @Valid @RequestBody InterviewDtos.RoundRequest request) {
        return interviewService.update(currentUser.require().id(), ResourceIds.parse(id), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String id) {
        interviewService.delete(currentUser.require().id(), ResourceIds.parse(id));
    }
}
