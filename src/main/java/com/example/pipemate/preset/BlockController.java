package com.example.pipemate.preset;

import com.example.pipemate.preset.res.BlockResponse;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/blocks")
@AllArgsConstructor
public class BlockController {

    private final BlockService blockService;

    // 전체 블록 조회
    @GetMapping
    public List<BlockResponse> getAllBlocks() {
        return blockService.getAllBlocks();
    }
}