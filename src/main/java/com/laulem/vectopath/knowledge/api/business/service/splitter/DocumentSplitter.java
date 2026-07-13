package com.laulem.vectopath.knowledge.api.business.service.splitter;

import java.util.List;

public interface DocumentSplitter {
    List<String> split(String content);
    String getSplitterKey();
}
