package com.lostsidewalk.buffy.app.query;

import com.lostsidewalk.buffy.app.model.request.FeedConfigRequest;
import com.lostsidewalk.buffy.app.model.request.RssAtomUrl;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class QueryCreationTask {

    List<RssAtomUrl> rssAtomUrls;

    String username;

    Long feedId;
}
