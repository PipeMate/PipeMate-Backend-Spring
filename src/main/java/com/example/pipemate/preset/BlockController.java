package com.example.pipemate.preset;

import com.example.pipemate.preset.res.BlockResponse;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/blocks")
public class BlockController {

    private final BlockService blockService;

    public BlockController(BlockService blockService) {
        this.blockService = blockService;
    }

    // 전체 블록 조회
    @GetMapping
    public List<BlockResponse> getAllBlocks() {
        return blockService.getAllBlocks();
    }
}