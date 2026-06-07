package com.sstt.ai;

import com.sstt.sources.SourceMessage;

public interface TaskExtractionPort {
    TaskExtractionResult extract(SourceMessage sourceMessage);
}
