/*
 * Copyright 2006 Nathanial X. Freitas, openvision.tv
 *
 * This code is currently released under the Mozilla Public License.
 * http://www.mozilla.org/MPL/
 *
 * Alternately you may apply the terms of the Apache Software License
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.rometools.modules.mediarss.io;

import com.rometools.modules.georss.GeoRSSModule;
import com.rometools.modules.georss.SimpleParser;
import com.rometools.modules.mediarss.MediaEntryModuleImpl;
import com.rometools.modules.mediarss.MediaModule;
import com.rometools.modules.mediarss.MediaModuleImpl;
import com.rometools.modules.mediarss.types.*;
import com.rometools.modules.mediarss.types.Embed.Param;
import com.rometools.modules.mediarss.types.Metadata.RightsStatus;
import com.rometools.rome.feed.module.Module;
import com.rometools.rome.io.ModuleParser;
import com.rometools.utils.Doubles;
import com.rometools.utils.Integers;
import com.rometools.utils.Longs;
import com.rometools.utils.Strings;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Long.parseLong;

/**
 * @author Nathanial X. Freitas
 *
 */
@SuppressWarnings("unused")
public class MediaModuleParser implements ModuleParser {

    private static final Logger LOG = LoggerFactory.getLogger(MediaModuleParser.class);

    private static final Namespace NS = Namespace.getNamespace(MediaModule.URI);

    private static final Pattern FILESIZE_WITH_UNIT_PATTERN = Pattern.compile("([\\d,.]+)([TGMK])?B", Pattern.CASE_INSENSITIVE);

    @Override
    public String getNamespaceUri() {
        return MediaModule.URI;
    }

    @Override
    public Module parse(final Element mmRoot, final Locale locale) {
        MediaModuleImpl mod;

        if (mmRoot.getName().equals("channel") || mmRoot.getName().equals("feed")) {
            mod = new MediaModuleImpl();
        } else {
            mod = new MediaEntryModuleImpl();
        }

        mod.setMetadata(parseMetadata(mmRoot, locale));
        PlayerReference player = parsePlayer(mmRoot);
        if (player != null) {
            mod.setPlayer(player);
        }

        if (mod instanceof final MediaEntryModuleImpl m) {
            m.setMediaContents(parseContent(mmRoot, locale));
            m.setMediaGroups(parseGroup(mmRoot, locale));
        }

        return mod;
    }

    static long parseFileSize(String fileSizeAttrValue) {
        String nonWSFileSize = fileSizeAttrValue.replaceAll("\\s", "");

        if(nonWSFileSize.matches("\\d+")) {
            return parseLong(nonWSFileSize);
        }

        Matcher sizeWithUnitMatcher = FILESIZE_WITH_UNIT_PATTERN.matcher(nonWSFileSize);
        if (sizeWithUnitMatcher.matches()) {
            BigDecimal number = new BigDecimal(sizeWithUnitMatcher.group(1).replace(',', '.'));
            BigDecimal multiplier = BigDecimal.valueOf(1);
            if (sizeWithUnitMatcher.group(2) != null) {
                char unit  = sizeWithUnitMatcher.group(2).toLowerCase().charAt(0);
                if (unit == 'k') {
                    multiplier = BigDecimal.valueOf(1000);
                } else if (unit == 'm') {
                    multiplier = BigDecimal.valueOf(1000).pow(2);
                } else if (unit == 'g') {
                    multiplier = BigDecimal.valueOf(1000).pow(3);
                } else if (unit == 't') {
                    multiplier = BigDecimal.valueOf(1000).pow(4);
                }
            }
            return number.multiply(multiplier).longValue();
        }

        throw new NumberFormatException("Invalid file size: " + fileSizeAttrValue);
    }

    /**
     * @param e element to parse
     * @param locale locale for parsing
     * @return array of media:content elements
     */
    private MediaContent[] parseContent(final Element e, final Locale locale) {

        final List<Element> contents = e.getChildren("content", getNS());
        final ArrayList<MediaContent> values = new ArrayList<>();

        try {
            for (int i = 0; contents != null && i < contents.size(); i++) {
                final Element content = contents.get(i);
                MediaContent mc = null;

                if (content.getAttributeValue("url") != null) {
                    try {
                        mc = new MediaContent(new UrlReference(new URI(content.getAttributeValue("url").replace(' ', '+'))));
                        mc.setPlayer(parsePlayer(content));
                    } catch (final Exception ex) {
                        LOG.warn("Exception parsing content tag due to: {}", ex.getMessage());
                    }
                } else {
                    PlayerReference player = parsePlayer(content);
                    if (player != null) {
                        mc = new MediaContent(player);
                    }
                }
                if (mc != null) {
                    values.add(mc);
                    try {
                        if (content.getAttributeValue("channels") != null) {
                            mc.setAudioChannels(parseInt(content.getAttributeValue("channels")));
                        }
                    } catch (final Exception ex) {
                        LOG.warn("Exception parsing content tag due to: {}", ex.getMessage());
                    }
                    try {
                        if (content.getAttributeValue("bitrate") != null) {
                            mc.setBitrate(Float.valueOf(content.getAttributeValue("bitrate")));
                        }
                    } catch (final Exception ex) {
                        LOG.warn("Exception parsing content tag due to: {}", ex.getMessage());
                    }
                    try {
                        if (content.getAttributeValue("duration") != null) {
                            mc.setDuration(Longs.parseDecimal(content.getAttributeValue("duration")));
                        }
                    } catch (final Exception ex) {
                        LOG.warn("Exception parsing content tag due to: {}", ex.getMessage());
                    }

                    mc.setMedium(content.getAttributeValue("medium"));

                    final String expression = content.getAttributeValue("expression");

                    if (expression != null) {
                        if (expression.equalsIgnoreCase("full")) {
                            mc.setExpression(Expression.FULL);
                        } else if (expression.equalsIgnoreCase("sample")) {
                            mc.setExpression(Expression.SAMPLE);
                        } else if (expression.equalsIgnoreCase("nonstop")) {
                            mc.setExpression(Expression.NONSTOP);
                        }
                    }

                    try {
                        if (content.getAttributeValue("fileSize") != null) {
                            mc.setFileSize(parseFileSize(content.getAttributeValue("fileSize")));
                        }
                    } catch (final Exception ex) {
                        LOG.warn("Exception parsing content tag due to: {}", ex.getMessage());
                    }
                    try {
                        if (content.getAttributeValue("framerate") != null) {
                            mc.setFramerate(Float.valueOf(content.getAttributeValue("framerate")));
                        }
                    } catch (final Exception ex) {
                        LOG.warn("Exception parsing content tag due to: {}", ex.getMessage());
                    }
                    try {
                        if (content.getAttributeValue("height") != null) {
                            mc.setHeight(parseInt(content.getAttributeValue("height")));
                        }
                    } catch (final Exception ex) {
                        LOG.warn("Exception parsing content tag due to: {}", ex.getMessage());
                    }

                    mc.setLanguage(content.getAttributeValue("lang"));
                    mc.setMetadata(parseMetadata(content, locale));
                    try {
                        if (content.getAttributeValue("samplingrate") != null) {
                            mc.setSamplingrate(Float.valueOf(content.getAttributeValue("samplingrate")));
                        }
                    } catch (final Exception ex) {
                        LOG.warn("Exception parsing content tag due to: {}", ex.getMessage());
                    }

                    mc.setType(content.getAttributeValue("type"));
                    try {
                        if (content.getAttributeValue("width") != null) {
                            mc.setWidth(parseInt(content.getAttributeValue("width")));
                        }
                    } catch (final Exception ex) {
                        LOG.warn("Exception parsing content tag due to: {}", ex.getMessage());
                    }

                    if (content.getAttributeValue("isDefault") != null) {
                        mc.setDefaultContent(parseBoolean(content.getAttributeValue("isDefault")));
                    }
                } else {
                    LOG.debug("Could not find MediaContent.");
                }

            }
        } catch (final Exception ex) {
            LOG.warn("Exception parsing content tag due to: {}", ex.getMessage());
        }

        return values.toArray(new MediaContent[0]);
    }

    /**
     * @param e element to parse
     * @param locale locale for parsing
     * @return array of media:group elements
     */
    private MediaGroup[] parseGroup(final Element e, final Locale locale) {
        final List<Element> groups = e.getChildren("group", getNS());
        final ArrayList<MediaGroup> values = new ArrayList<>();

        for (int i = 0; groups != null && i < groups.size(); i++) {
            final Element group = groups.get(i);
            final MediaGroup g = new MediaGroup(parseContent(group, locale));

            for (int j = 0; j < g.getContents().length; j++) {
                if (g.getContents()[j].isDefaultContent()) {
                    g.setDefaultContentIndex(j);

                    break;
                }
            }

            g.setMetadata(parseMetadata(group, locale));
            values.add(g);
        }

        return values.toArray(new MediaGroup[0]);
    }

    /**
     * @param e element to parse
     * @param locale locale for parsing
     * @return Metadata of media:group or media:content
     */
    private Metadata parseMetadata(final Element e, final Locale locale) {
        final Metadata md = new Metadata();
        parseCategories(e, md);
        parseCopyright(e, md);
        parseCredits(e, md);
        parseDescription(e, md);
        parseHash(e, md);
        parseKeywords(e, md);
        parseRatings(e, md);
        parseText(e, md);
        parseThumbnail(e, md);
        parseTitle(e, md);
        parseRestrictions(e, md);
        parseAdultMetadata(e, md);
        parseBackLinks(e, md);
        parseComments(e, md);
        parseCommunity(e, md);
        parsePrices(e, md);
        parseResponses(e, md);
        parseStatus(e, md);
        parseEmbed(e, md);
        parseLicenses(e, md);
        parseSubTitles(e, md);
        parsePeerLinks(e, md);
        parseRights(e, md);
        parseLocations(e, md, locale);
        parseScenes(e, md);
        return md;
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parseScenes(final Element e, final Metadata md) {
        final Element scenesElement = e.getChild("scenes", getNS());
        if (scenesElement != null) {
            final List<Element> sceneElements = scenesElement.getChildren("scene", getNS());
            final Scene[] scenes = new Scene[sceneElements.size()];
            for (int i = 0; i < sceneElements.size(); i++) {
                scenes[i] = new Scene();
                scenes[i].setTitle(sceneElements.get(i).getChildText("sceneTitle", getNS()));
                scenes[i].setDescription(sceneElements.get(i).getChildText("sceneDescription", getNS()));
                final String sceneStartTime = sceneElements.get(i).getChildText("sceneStartTime", getNS());
                if (sceneStartTime != null) {
                    scenes[i].setStartTime(new Time(sceneStartTime));
                }
                final String sceneEndTime = sceneElements.get(i).getChildText("sceneEndTime", getNS());
                if (sceneEndTime != null) {
                    scenes[i].setEndTime(new Time(sceneEndTime));
                }
            }
            md.setScenes(scenes);
        }
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     * @param locale locale for parser
     */
    private void parseLocations(final Element e, final Metadata md, final Locale locale) {
        final List<Element> locationElements = e.getChildren("location", getNS());
        final Location[] locations = new Location[locationElements.size()];
        final SimpleParser geoRssParser = new SimpleParser();
        for (int i = 0; i < locationElements.size(); i++) {
            locations[i] = new Location();
            locations[i].setDescription(locationElements.get(i).getAttributeValue("description"));
            if (locationElements.get(i).getAttributeValue("start") != null) {
                locations[i].setStart(new Time(locationElements.get(i).getAttributeValue("start")));
            }
            if (locationElements.get(i).getAttributeValue("end") != null) {
                locations[i].setEnd(new Time(locationElements.get(i).getAttributeValue("end")));
            }
            final Module geoRssModule = geoRssParser.parse(locationElements.get(i), locale);
            if (geoRssModule instanceof GeoRSSModule) {
                locations[i].setGeoRss((GeoRSSModule) geoRssModule);
            }
        }
        md.setLocations(locations);
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parseRights(final Element e, final Metadata md) {
        final Element rightsElement = e.getChild("rights", getNS());
        if (rightsElement != null && rightsElement.getAttributeValue("status") != null) {
            md.setRights(RightsStatus.valueOf(rightsElement.getAttributeValue("status")));
        }
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parsePeerLinks(final Element e, final Metadata md) {
        final List<Element> peerLinkElements = e.getChildren("peerLink", getNS());
        final PeerLink[] peerLinks = new PeerLink[peerLinkElements.size()];
        for (int i = 0; i < peerLinkElements.size(); i++) {
            peerLinks[i] = new PeerLink();
            peerLinks[i].setType(peerLinkElements.get(i).getAttributeValue("type"));
            if (peerLinkElements.get(i).getAttributeValue("href") != null) {
                try {
                    peerLinks[i].setHref(new URL(peerLinkElements.get(i).getAttributeValue("href")));
                } catch (MalformedURLException ex) {
                    LOG.warn("Exception parsing peerLink href attribute due to: {}", ex.getMessage());
                }
            }
        }
        md.setPeerLinks(peerLinks);
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parseSubTitles(final Element e, final Metadata md) {
        final List<Element> subTitleElements = e.getChildren("subTitle", getNS());
        final SubTitle[] subtitles = new SubTitle[subTitleElements.size()];
        for (int i = 0; i < subTitleElements.size(); i++) {
            subtitles[i] = new SubTitle();
            subtitles[i].setType(subTitleElements.get(i).getAttributeValue("type"));
            subtitles[i].setLang(subTitleElements.get(i).getAttributeValue("lang"));
            if (subTitleElements.get(i).getAttributeValue("href") != null) {
                try {
                    subtitles[i].setHref(new URL(subTitleElements.get(i).getAttributeValue("href")));
                } catch (MalformedURLException ex) {
                    LOG.warn("Exception parsing subTitle href attribute due to: {}", ex.getMessage());
                }
            }
        }
        md.setSubTitles(subtitles);
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parseLicenses(final Element e, final Metadata md) {
        final List<Element> licenseElements = e.getChildren("license", getNS());
        final License[] licenses = new License[licenseElements.size()];
        for (int i = 0; i < licenseElements.size(); i++) {
            licenses[i] = new License();
            licenses[i].setType(licenseElements.get(i).getAttributeValue("type"));
            licenses[i].setValue(licenseElements.get(i).getTextTrim());
            if (licenseElements.get(i).getAttributeValue("href") != null) {
                try {
                    licenses[i].setHref(new URL(licenseElements.get(i).getAttributeValue("href")));
                } catch (MalformedURLException ex) {
                    LOG.warn("Exception parsing license href attribute due to: {}", ex.getMessage());
                }
            }
        }
        md.setLicenses(licenses);
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parsePrices(final Element e, final Metadata md) {
        final List<Element> priceElements = e.getChildren("price", getNS());
        final Price[] prices = new Price[priceElements.size()];
        for (int i = 0; i < priceElements.size(); i++) {

            final Element priceElement = priceElements.get(i);
            prices[i] = new Price();

            final String currency = priceElement.getAttributeValue("currency");
            if (currency != null) {
                try {
                    prices[i].setCurrency(Currency.getInstance(currency));
                } catch (IllegalArgumentException ex) {
                    LOG.warn("Invalid currency", ex);
                }
            }

            final String price = priceElement.getAttributeValue("price");
            if (price != null) {
                try {
                    prices[i].setPrice(new BigDecimal(price));
                } catch (NumberFormatException ex) {
                    LOG.warn("Invalid price", ex);
                }
            }

            if (priceElement.getAttributeValue("type") != null) {
                prices[i].setType(Price.Type.valueOf(priceElement.getAttributeValue("type").toUpperCase()));
            }
            if (priceElement.getAttributeValue("info") != null) {
                try {
                    prices[i].setInfo(new URL(priceElement.getAttributeValue("info")));
                } catch (MalformedURLException ex) {
                    LOG.warn("Exception parsing price info attribute due to: {}", ex.getMessage());
                }
            }
        }
        md.setPrices(prices);
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parseStatus(final Element e, final Metadata md) {
        final Element statusElement = e.getChild("status", getNS());
        if (statusElement != null) {
            final Status status = new Status();
            status.setState(Status.State.valueOf(statusElement.getAttributeValue("state")));
            status.setReason(statusElement.getAttributeValue("reason"));
            md.setStatus(status);
        }
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parseBackLinks(final Element e, final Metadata md) {
        final Element backLinksElement = e.getChild("backLinks", getNS());
        if (backLinksElement != null) {
            final List<Element> backLinkElements = backLinksElement.getChildren("backLink", getNS());
            final URL[] backLinks = new URL[backLinkElements.size()];
            for (int i = 0; i < backLinkElements.size(); i++) {
                try {
                    backLinks[i] = new URL(backLinkElements.get(i).getTextTrim());
                } catch (MalformedURLException ex) {
                    LOG.warn("Exception parsing backLink tag due to: {}", ex.getMessage());
                }
            }
            md.setBackLinks(backLinks);
        }
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parseComments(final Element e, final Metadata md) {
        final Element commentsElement = e.getChild("comments", getNS());
        if (commentsElement != null) {
            final List<Element> commentElements = commentsElement.getChildren("comment", getNS());
            final String[] comments = new String[commentElements.size()];
            for (int i = 0; i < commentElements.size(); i++) {
                comments[i] = commentElements.get(i).getTextTrim();
            }
            md.setComments(comments);
        }
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parseResponses(final Element e, final Metadata md) {
        final Element responsesElement = e.getChild("responses", getNS());
        if (responsesElement != null) {
            final List<Element> responseElements = responsesElement.getChildren("response", getNS());
            final String[] responses = new String[responseElements.size()];
            for (int i = 0; i < responseElements.size(); i++) {
                responses[i] = responseElements.get(i).getTextTrim();
            }
            md.setResponses(responses);
        }
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parseCommunity(final Element e, final Metadata md) {
        final Element communityElement = e.getChild("community", getNS());
        if (communityElement != null) {
            final Community community = new Community();
            final Element starRatingElement = communityElement.getChild("starRating", getNS());
            if (starRatingElement != null) {
                final StarRating starRating = new StarRating();
                starRating.setAverage(Doubles.parse(starRatingElement.getAttributeValue("average")));
                starRating.setCount(Integers.parse(starRatingElement.getAttributeValue("count")));
                starRating.setMax(Integers.parse(starRatingElement.getAttributeValue("max")));
                starRating.setMin(Integers.parse(starRatingElement.getAttributeValue("min")));
                community.setStarRating(starRating);
            }
            final Element statisticsElement = communityElement.getChild("statistics", getNS());
            if (statisticsElement != null) {
                final Statistics statistics = new Statistics();
                statistics.setFavorites(Integers.parse(statisticsElement.getAttributeValue("favorites")));
                statistics.setViews(Integers.parse(statisticsElement.getAttributeValue("views")));
                community.setStatistics(statistics);
            }
            final Element tagsElement = communityElement.getChild("tags", getNS());
            if (tagsElement != null) {
                final String tagsText = tagsElement.getTextTrim();
                final String[] tags = tagsText.split("\\s*,\\s*");
                for (String tagText : tags) {
                    final String[] tagParts = tagText.split("\\s*:\\s*");
                    final Tag tag = new Tag(tagParts[0]);
                    if (tagParts.length > 1) {
                        tag.setWeight(Integers.parse(tagParts[1]));
                    }
                    community.getTags().add(tag);
                }
            }
            md.setCommunity(community);
        }
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parseCategories(final Element e, final Metadata md) {
        final List<Element> categories = e.getChildren("category", getNS());
        final ArrayList<Category> values = new ArrayList<>();

        for (int i = 0; categories != null && i < categories.size(); i++) {
            try {
                final Element cat = categories.get(i);
                values.add(new Category(cat.getAttributeValue("scheme"), cat.getAttributeValue("label"), cat.getText()));
            } catch (final Exception ex) {
                LOG.warn("Exception parsing category tag due to: {}", ex.getMessage());
            }
        }

        md.setCategories(values.toArray(new Category[0]));
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parseCopyright(final Element e, final Metadata md) {
        try {
            final Element copy = e.getChild("copyright", getNS());

            if (copy != null) {
                md.setCopyright(copy.getText());
                if (copy.getAttributeValue("url") != null) {
                    md.setCopyrightUrl(new URI(copy.getAttributeValue("url")));
                }
            }
        } catch (final Exception ex) {
            LOG.warn("Exception parsing copyright tag due to: {}", ex.getMessage());
        }
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parseCredits(final Element e, final Metadata md) {
        final List<Element> credits = e.getChildren("credit", getNS());
        final ArrayList<Credit> values = new ArrayList<>();

        for (int i = 0; credits != null && i < credits.size(); i++) {
            try {
                final Element cred = credits.get(i);
                values.add(new Credit(cred.getAttributeValue("scheme"), cred.getAttributeValue("role"), cred.getText()));
                md.setCredits(values.toArray(new Credit[0]));
            } catch (final Exception ex) {
                LOG.warn("Exception parsing credit tag due to: {}", ex.getMessage());
            }
        }
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parseDescription(final Element e, final Metadata md) {
        try {
            final Element description = e.getChild("description", getNS());

            if (description != null) {
                md.setDescription(description.getText());
                md.setDescriptionType(description.getAttributeValue("type"));
            }
        } catch (final Exception ex) {
            LOG.warn("Exception parsing description tag due to: {}", ex.getMessage());
        }
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parseEmbed(final Element e, final Metadata md) {
        final Element embedElement = e.getChild("embed", getNS());
        if (embedElement != null) {
            final Embed embed = new Embed();
            embed.setWidth(Integers.parse(embedElement.getAttributeValue("width")));
            embed.setHeight(Integers.parse(embedElement.getAttributeValue("height")));
            if (embedElement.getAttributeValue("url") != null) {
                try {
                    embed.setUrl(new URL(embedElement.getAttributeValue("url")));
                } catch (MalformedURLException ex) {
                    LOG.warn("Exception parsing embed tag due to: {}", ex.getMessage());
                }
            }
            final List<Element> paramElements = embedElement.getChildren("param", getNS());
            embed.setParams(new Param[paramElements.size()]);
            for (int i = 0; i < paramElements.size(); i++) {
                embed.getParams()[i] = new Param(paramElements.get(i).getAttributeValue("name"), paramElements.get(i).getTextTrim());
            }
            md.setEmbed(embed);
        }
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parseHash(final Element e, final Metadata md) {
        try {
            final Element hash = e.getChild("hash", getNS());

            if (hash != null) {
                md.setHash(new Hash(hash.getAttributeValue("algo"), hash.getText()));
            }
        } catch (final Exception ex) {
            LOG.warn("Exception parsing hash tag.", ex);
        }
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parseKeywords(final Element e, final Metadata md) {
        final Element keywords = e.getChild("keywords", getNS());

        if (keywords != null) {
            final StringTokenizer tok = new StringTokenizer(keywords.getText(), ",");
            final String[] value = new String[tok.countTokens()];

            for (int i = 0; tok.hasMoreTokens(); i++) {
                value[i] = tok.nextToken().trim();
            }

            md.setKeywords(value);
        }
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parseRatings(final Element e, final Metadata md) {
        final ArrayList<Rating> values = new ArrayList<>();

        final List<Element> ratings = e.getChildren("rating", getNS());
        for (final Element ratingElement : ratings) {
            try {
                final String ratingText = ratingElement.getText();
                String ratingScheme = Strings.trimToNull(ratingElement.getAttributeValue("scheme"));
                if (ratingScheme == null) {
                    ratingScheme = "urn:simple";
                }
                final Rating rating = new Rating(ratingScheme, ratingText);
                values.add(rating);
            } catch (final Exception ex) {
                LOG.warn("Exception parsing rating tag due to: {}", ex.getMessage());
            }
        }

        md.setRatings(values.toArray(new Rating[0]));

    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parseText(final Element e, final Metadata md) {
        final List<Element> texts = e.getChildren("text", getNS());
        final ArrayList<Text> values = new ArrayList<>();

        for (int i = 0; texts != null && i < texts.size(); i++) {
            try {
                final Element text = texts.get(i);
                Time start = null;
                if (text.getAttributeValue("start") != null) {
                    start = new Time(text.getAttributeValue("start"));
                }
                Time end = null;
                if (text.getAttributeValue("end") != null) {
                    end = new Time(text.getAttributeValue("end"));
                }
                values.add(new Text(text.getAttributeValue("type"), text.getTextTrim(), start, end));
            } catch (final Exception ex) {
                LOG.warn("Exception parsing text tag due to: {}", ex.getMessage());
            }
        }

        md.setText(values.toArray(new Text[0]));
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parseThumbnail(final Element e, final Metadata md) {
        final ArrayList<Thumbnail> values = new ArrayList<>();

        final List<Element> thumbnails = e.getChildren("thumbnail", getNS());
        for (final Element thumb : thumbnails) {
            try {

                final String timeAttr = Strings.trimToNull(thumb.getAttributeValue("time"));
                Time time = null;
                if (timeAttr != null) {
                    time = new Time(timeAttr);
                }

                final String widthAttr = thumb.getAttributeValue("width");
                final Integer width = Integers.parse(widthAttr);

                final String heightAttr = thumb.getAttributeValue("height");
                final Integer height = Integers.parse(heightAttr);

                final String url = thumb.getAttributeValue("url");
                final URI uri = new URI(url);
                final Thumbnail thumbnail = new Thumbnail(uri, width, height, time);

                values.add(thumbnail);

            } catch (final Exception ex) {
                LOG.warn("Exception parsing thumbnail tag due to: {}", ex.getMessage());
            }
        }

        md.setThumbnail(values.toArray(new Thumbnail[0]));
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parseTitle(final Element e, final Metadata md) {
        final Element title = e.getChild("title", getNS());

        if (title != null) {
            md.setTitle(title.getText());
            md.setTitleType(title.getAttributeValue("type"));
        }
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parseRestrictions(final Element e, final Metadata md) {
        final List<Element> restrictions = e.getChildren("restriction", getNS());
        final ArrayList<Restriction> values = new ArrayList<>();

        for (final Element r : restrictions) {
            Restriction.Type type = null;
            String restrictionType = r.getAttributeValue("type");
            if (restrictionType != null) {
                if (restrictionType.equalsIgnoreCase(Restriction.Type.URI.toString())) {
                    type = Restriction.Type.URI;
                } else if (restrictionType.equalsIgnoreCase(Restriction.Type.COUNTRY.toString())) {
                    type = Restriction.Type.COUNTRY;
                } else if (restrictionType.equalsIgnoreCase(Restriction.Type.SHARING.toString())) {
                    type = Restriction.Type.SHARING;
                }
            }

            Restriction.Relationship relationship = null;

            if (r.getAttributeValue("relationship").equalsIgnoreCase("allow")) {
                relationship = Restriction.Relationship.ALLOW;
            } else if (r.getAttributeValue("relationship").equalsIgnoreCase("deny")) {
                relationship = Restriction.Relationship.DENY;
            }

            assert relationship != null;
            final Restriction value;
            value = new Restriction(relationship, type, r.getTextTrim());
            values.add(value);
        }

        md.setRestrictions(values.toArray(new Restriction[0]));
    }

    /**
     * @param e element to parse
     * @param md metadata to fill in
     */
    private void parseAdultMetadata(final Element e, final Metadata md) {
        final Element adult = e.getChild("adult", getNS());

        if (adult != null && md.getRatings().length == 0) {
            final Rating[] r = new Rating[1];

            if (adult.getTextTrim().equals("true")) {
                r[0] = new Rating("urn:simple", "adult");
            } else {
                r[0] = new Rating("urn:simple", "nonadult");
            }

            md.setRatings(r);
        }
    }

    /**
     * @param e element to parse
     * @return PlayerReference element
     */
    private PlayerReference parsePlayer(final Element e) {
        final Element player = e.getChild("player", getNS());
        PlayerReference p = null;

        if (player != null) {
            try {
                Integer width = null;
                if (player.getAttributeValue("width") != null) {
                    width = parseInt(player.getAttributeValue("width"));
                }
                Integer height = null;
                if (player.getAttributeValue("height") != null) {
                    height = parseInt(player.getAttributeValue("height"));
                }
                if (player.getAttributeValue("url") != null) {
                    p = new PlayerReference(new URI(player.getAttributeValue("url")), width, height);
                } else {
                    LOG.debug("Player tag is missing URL");
                }
            } catch (final Exception ex) {
                LOG.warn("Exception parsing player tag due to: {}", ex.getMessage());
            }
        }

        return p;
    }

    public Namespace getNS() {
        return NS;
    }

    private static Integer parseInt(String str) {
        if (isBlank(str)) {
            return null;
        }
        String s = str.replaceAll("[^d]", "");
        if (isNumeric(s)) {
            return Integer.parseInt(s);
        }
        return null;
    }
}
