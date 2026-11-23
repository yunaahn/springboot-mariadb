package com.example.app.memo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/memos")
public class MemoController {

    private final MemoRepository memoRepository;

    public MemoController(MemoRepository memoRepository) {
        this.memoRepository = memoRepository;
    }

    @GetMapping
    public List<Memo> list() {
        return memoRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Memo create(@Valid @RequestBody CreateRequest request) {
        return memoRepository.save(new Memo(request.content()));
    }

    public record CreateRequest(@NotBlank @Size(max = 200) String content) {
    }
}
