package com.example.pipemate.pipeline.converter;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.representer.Representer;

public class PrettyRepresenter extends Representer {
    public PrettyRepresenter(DumperOptions options) {
        super(options);
        this.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK;
    }
}