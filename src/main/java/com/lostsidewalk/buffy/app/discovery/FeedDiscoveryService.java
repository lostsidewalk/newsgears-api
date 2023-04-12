package com.lostsidewalk.buffy.app.discovery;

import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo;
import com.lostsidewalk.buffy.discovery.FeedDiscoveryInfo.FeedDiscoveryException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lostsidewalk.buffy.rss.RssDiscovery.discoverUrl;
import static java.util.List.copyOf;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.collections4.CollectionUtils.size;

@Slf4j
@Service
public class FeedDiscoveryService {

    @Value("${newsgears.userAgent}")
    String feedGearsUserAgent;
    //
    // INDIVIDUAL URL DISCOVERY
    //
    @Cacheable(value="feedDiscoveryCache")
    public FeedDiscoveryInfo performDiscovery(String url, String username, String password) throws FeedDiscoveryException {
        return discoverUrl(url, username, password, feedGearsUserAgent);
    }
    //
    // COLLECTION DISCOVERY
    //
    private static final List<String> TOP_NEWS_URLS = List.of(
        "http://rss.news.yahoo.com/rss/topstories",
        "https://www.telegraph.co.uk/rss.xml",
        "http://feeds.nbcnews.com/nbcnews/public/news",
        "http://feeds.abcnews.com/abcnews/topstories",
        "http://feeds.feedburner.com/foxnews/latest",
        "http://www.independent.co.uk/rss",
        "https://www.huffpost.com/section/front-page/feed",
        "http://feeds.washingtonpost.com/rss/world",
        "http://feeds.guardian.co.uk/theguardian/world/rss",
        "https://time.com/feed/",
        "https://news.google.com/news/rss/?gl=US&ned=us&hl=en",
        "http://www.nytimes.com/services/xml/rss/nyt/HomePage.xml",
        "https://feeds.a.dj.com/rss/RSSWorldNews.xml",
        "http://feeds.bbci.co.uk/news/rss.xml",
        "http://rss.cnn.com/rss/cnn_topstories.rss"
    );

    private static final List<String> INVESTING_URLS = List.of(
        "https://www.thestreet.com/.rss/full/",
        "https://www.investing.com/rss/news_25.rss",
        "https://seekingalpha.com/feed.xml",
        "https://www.cnbc.com/id/15839069/device/rss/rss.html",
        "https://www.fool.com/a/feeds/foolwatch?format=rss2&id=foolwatch&apikey=foolwatch-feed",
        "http://feeds.marketwatch.com/marketwatch/marketpulse/",
        "https://www.ft.com/markets?format=rss"
    );

    private static final List<String> BUSINESS_URLS = List.of(
         "https://startupnation.com/feed",
         "https://avc.com/feed/",
         "https://www.killerstartups.com/feed/",
         "https://justinjackson.ca/feed",
         "http://feeds.feedburner.com/SmallBusinessTrends?format=xml",
         "https://www.youtube.com/feeds/videos.xml?channel_id=UCkkhmBWfS7pILYIk0izkc3A",
         "https://tomtunguz.com/index.xml",
         "https://feld.com/feed",
         "http://feeds.feedburner.com/Onlyonce",
         "https://steveblank.com/feed/",
         "http://blog.asmartbear.com/feed",
         "http://feeds.feedburner.com/forentrepreneurs",
         "https://bothsidesofthetable.com/feed",
         "http://www.kickstarter.com/blog.atom",
         "http://www.forbes.com/entrepreneurs/index.xml",
         "http://blog.samaltman.com/posts.atom",
         "https://www.inc.com/rss/homepage.xml",
         "https://venturebeat.com/feed/",
         "http://www.fastcompany.com/rss.xml",
         "http://feeds.feedburner.com/entrepreneur/latest"
    );

    private static final List<String> TECHNOLOGY_URLS = List.of(
         "https://medium.com/feed/gigaom",
         "https://techviral.net/feed/",
         "https://www.techrepublic.com/rssfeeds/articles/",
         "https://www.nytimes.com/svc/collections/v1/publish/https://www.nytimes.com/section/technology/rss.xml",
         "https://www.techradar.com/rss",
         "https://www.geekwire.com/feed/",
         "https://www.digitaltrends.com/feed/",
         "http://feeds.mashable.com/mashable/tech",
         "http://futurism.com/feed",
         "http://www.recode.net/rss/index.xml",
         "https://www.theinformation.com/feed",
         "http://rss.slashdot.org/Slashdot/slashdotMain",
         "https://www.cnet.com/rss/news/",
         "http://9to5mac.com/feed/",
         "http://feeds2.feedburner.com/thenextweb",
         "http://www.macrumors.com/macrumors.xml",
         "https://news.ycombinator.com/rss",
         "https://techcrunch.com/feed/",
         "http://feeds.wired.com/wired/index",
         "https://www.theverge.com/rss/index.xml",
         "http://feeds.arstechnica.com/arstechnica/index",
         "https://www.engadget.com/rss.xml"
    );

    private static final List<String> SCIENCE_URLS = List.of(
         "https://www.futurity.org/feed/",
         "https://www.sciencedaily.com/rss/top.xml",
         "https://www.livescience.com/feeds/all",
         "https://phys.org/rss-feed/",
         "https://www.science.org/rss/news_current.xml",
         "https://www.technologyreview.com/topnews.rss",
         "https://api.quantamagazine.org/feed/",
         "https://www.wired.com/feed/category/science/latest/rss",
         "http://www.nytimes.com/services/xml/rss/nyt/Science.xml",
         "http://feeds.newscientist.com/science-news",
         "http://newsrss.bbc.co.uk/rss/newsonline_world_edition/science/nature/rss.xml",
         "https://www.popsci.com/feed/",
         "http://www.economist.com/rss/science_and_technology_rss.xml",
         "http://rss.sciam.com/ScientificAmerican-Global",
         "https://www.ted.com/feeds/talks.rss"
    );

    private static final List<String> CREATIVE_URLS = List.of(
         "https://www.dexigner.com/feed/news",
         "https://www.dezeen.com/design/feed/",
         "http://www.independent.co.uk/arts-entertainment/art/rss",
         "https://www.designboom.com/feed/",
         "https://www.designernews.co/?format=atom",
         "http://feeds.feedburner.com/DailyDesignerNews",
         "http://feeds.feedburner.com/core77/blog",
         "http://www.yankodesign.com/feed/"
    );

    private static final List<String> LIFESTYLE_URLS = List.of(
         "http://www.ebaumsworld.com/rss/featured/",
         "http://www.youtube.com/feeds/videos.xml?channel_id=UCfAOh2t5DpxVrgS9NQKjC7A",
         "https://www.youtube.com/feeds/videos.xml?channel_id=UCPDXXXJj9nax0fr0Wfc048g",
         "http://feeds.feedburner.com/feedburner/ZdSV",
         "http://feeds.feedburner.com/ICanHasCheezburger",
         "http://www.reddit.com/.rss",
         "http://feeds.feedburner.com/BoredPanda",
         "http://feeds.feedburner.com/CrackedRSS",
         "http://9gagrss.com/feed/",
         "http://feeds.feedburner.com/failblog"
    );

    private static final String TOP_NEWS = "topNews";
    private static final String INVESTING = "investing";
    private static final String BUSINESS = "business";
    private static final String TECHNOLOGY = "technology";
    private static final String SCIENCE = "science";
    private static final String CREATIVE = "creative";
    private static final String LIFESTYLE = "lifestyle";

    private static final Map<String, List<String>> COLLECTION_URLS = new HashMap<>() {
        {
            put(TOP_NEWS, TOP_NEWS_URLS);
            put(INVESTING, INVESTING_URLS);
            put(BUSINESS, BUSINESS_URLS);
            put(TECHNOLOGY, TECHNOLOGY_URLS);
            put(SCIENCE, SCIENCE_URLS);
            put(CREATIVE, CREATIVE_URLS);
            put(LIFESTYLE, LIFESTYLE_URLS);
        }
    };

    static volatile Map<String, List<FeedDiscoveryInfo>> FEED_DISCOVERY_INFO_CACHE = new HashMap<>();

    @Value("${newsgears.development:false}")
    boolean isDevelopment;

    @PostConstruct
    public void postConstruct() {
        if (!isDevelopment) {
            try {
                FEED_DISCOVERY_INFO_CACHE.put(TOP_NEWS, performCollectionDiscovery(TOP_NEWS));
                FEED_DISCOVERY_INFO_CACHE.put(INVESTING, performCollectionDiscovery(INVESTING));
                FEED_DISCOVERY_INFO_CACHE.put(BUSINESS, performCollectionDiscovery(BUSINESS));
                FEED_DISCOVERY_INFO_CACHE.put(TECHNOLOGY, performCollectionDiscovery(TECHNOLOGY));
                FEED_DISCOVERY_INFO_CACHE.put(SCIENCE, performCollectionDiscovery(SCIENCE));
                FEED_DISCOVERY_INFO_CACHE.put(CREATIVE, performCollectionDiscovery(CREATIVE));
                FEED_DISCOVERY_INFO_CACHE.put(LIFESTYLE, performCollectionDiscovery(LIFESTYLE));
                log.info("Full feed discovery service initialized");
            } catch (Exception e) {
                log.warn("Unable to perform collection discovery due to: {}", e.getMessage());
            }
        } else {
            try {
                FEED_DISCOVERY_INFO_CACHE.put(TOP_NEWS, performCollectionDiscovery(TOP_NEWS));
                log.info("Minimal feed discovery service initialized");
            } catch (Exception e) {
                log.warn("Unable to perform collection discovery due to: {}", e.getMessage());
            }
        }
    }

    private List<FeedDiscoveryInfo> performCollectionDiscovery(@SuppressWarnings("SameParameterValue") String collectionName) {
        List<FeedDiscoveryInfo> results = null;
        List<String> urls = COLLECTION_URLS.get(collectionName);
        if (isNotEmpty(urls)) {
            log.info("Performing collection discovery for collection={}, urlCt={}", collectionName, size(urls));
            results = new ArrayList<>(size(urls));
            for (String url : urls) {
                try {
                    FeedDiscoveryInfo f = performDiscovery(url, null, null);
                    if (f != null && f.getErrorType() == null) {
                        results.add(f);
                    }
                } catch (FeedDiscoveryException e) {
                    log.error("Unable to perform collection discovery on URL={}, due to: {}", url, e.getMessage());
                }
            }
        }
        log.info("Collection discovery complete for collection={}, resultCt={}, errorCt={}", collectionName, size(results), size(urls) - size(results));
        return results;
    }

    public List<FeedDiscoveryInfo> getCollection(String collectionName) {
        List<FeedDiscoveryInfo> collection = FEED_DISCOVERY_INFO_CACHE.get(collectionName);
        if (collection != null) {
            return copyOf(collection);
        }
        return null;
    }
}
