package com.vinh2201.youtubesmarttvandroid44;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Keep;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayInputStream;

public class MainActivity extends AppCompatActivity {

    private WebView webView;

    // ---- Robust JS bridge (callable from JS even with minify) ----
    @Keep
    public static class ExitJsBridge {
        private final MainActivity activity;
        ExitJsBridge(MainActivity a){ this.activity = a; }

        @JavascriptInterface
        @SuppressWarnings("unused")
        public void onExitCalled() {
            activity.runOnUiThread(activity::finishAffinity);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings ws = webView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);
        ws.setUseWideViewPort(true);
        ws.setLoadWithOverviewMode(true);
        ws.setSupportZoom(false);
        ws.setUserAgentString("Mozilla/5.0 (Linux; Android 14; Android TV Build/UP1A.231005.007) Cobalt/25.0");

        // resize UI page to fit screen
        ws.setTextZoom(100);
        webView.setInitialScale(25);

        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);

        // JS bridge
        webView.addJavascriptInterface(new ExitJsBridge(this), "ExitBridge");

        // --- 1. Spoof Viewport (Từ spoof_viewport.js) ---
        final String spoofViewportJS = "javascript:(function() {"
                + "var existing = document.querySelector('meta[name=\"viewport\"]');"
                + "if (existing) {"
                + "    existing.setAttribute('content', 'width=3840, height=2160, initial-scale=1.0');"
                + "} else {"
                + "    var meta = document.createElement('meta');"
                + "    meta.name = 'viewport';"
                + "    meta.content = 'width=3840, height=2160, initial-scale=1.0';"
                + "    document.head.appendChild(meta);"
                + "}"
                + "})();";

        // --- 2. Main Userscript (Từ userscript.js) ---
        final String pageJS = "javascript:(function () {"
                + "\"use strict\";"
                + "if (window.__notubetvLoaded) return;"
                + "window.__notubetvLoaded = true;"
                + "const CONFIG_KEY = \"ytaf-configuration\";"
                + "const defaultConfig = {"
                + "    enableAdBlock: true,"
                + "    enableSponsorBlock: true,"
                + "    sponsorBlockManualSkips: [],"
                + "    enableSponsorBlockSponsor: true,"
                + "    enableSponsorBlockIntro: true,"
                + "    enableSponsorBlockOutro: true,"
                + "    enableSponsorBlockInteraction: true,"
                + "    enableSponsorBlockSelfPromo: true,"
                + "    enableSponsorBlockMusicOfftopic: false,"
                + "    enableShorts: true,"
                + "    enableForceAvc1: true"
                + "};"
                + "let localConfig;"
                + "try {"
                + "    localConfig = JSON.parse(window.localStorage[CONFIG_KEY]);"
                + "} catch (err) {"
                + "    localConfig = { ...defaultConfig };"
                + "    try { window.localStorage[CONFIG_KEY] = JSON.stringify(localConfig); } catch (e) {}"
                + "}"
                + "window.localConfig = localConfig;"
                + "window.configRead = function (key) {"
                + "    if (window.localConfig[key] === undefined) { window.localConfig[key] = defaultConfig[key]; }"
                + "    return window.localConfig[key];"
                + "};"
                + "window.configWrite = function (key, value) {"
                + "    window.localConfig[key] = value;"
                + "    window.localStorage[CONFIG_KEY] = JSON.stringify(window.localConfig);"
                + "    if (key === \"enableShorts\") { try { applyShortsHidingCSS(); } catch (err) {} }"
                + "};"
                + "function showToast(title, subtitle) {"
                + "    const toastCmd = { openPopupAction: { popupType: \"TOAST\", popup: { overlayToastRenderer: { title: { simpleText: title }, subtitle: { simpleText: subtitle } } } } };"
                + "    resolveCommand(toastCmd);"
                + "}"
                + "function showModal(title, content, selectIndex, id, update) {"
                + "    if (!update) { resolveCommand({ signalAction: { signal: \"POPUP_BACK\" } }); }"
                + "    const modalCmd = { openPopupAction: { popupType: \"MODAL\", popup: { overlaySectionRenderer: { overlay: { overlayTwoPanelRenderer: { actionPanel: { overlayPanelRenderer: { header: { overlayPanelHeaderRenderer: { title: { simpleText: title } } }, content: { overlayPanelItemListRenderer: { items: content, selectedIndex: selectIndex } } } }, backButton: { buttonRenderer: { accessibilityData: { accessibilityData: { label: \"Back\" } }, command: { signalAction: { signal: \"POPUP_BACK\" } } } } } }, dismissalCommand: { signalAction: { signal: \"POPUP_BACK\" } } } }, uniqueId: id } };"
                + "    if (update) { modalCmd.openPopupAction.shouldMatchUniqueId = true; modalCmd.openPopupAction.updateAction = true; }"
                + "    resolveCommand(modalCmd);"
                + "}"
                + "function buttonItem(title, icon, commands) {"
                + "    const button = { compactLinkRenderer: { serviceEndpoint: { commandExecutorCommand: { commands } } } };"
                + "    if (title) { button.compactLinkRenderer.title = { simpleText: title.title }; }"
                + "    if (title && title.subtitle) { button.compactLinkRenderer.subtitle = { simpleText: title.subtitle }; }"
                + "    if (icon) { button.compactLinkRenderer.icon = { iconType: icon.icon }; }"
                + "    if (icon && icon.secondaryIcon) { button.compactLinkRenderer.secondaryIcon = { iconType: icon.secondaryIcon }; }"
                + "    return button;"
                + "}"
                + "window.modernUI = function modernUI(update, parameters) {"
                + "    const settings = ["
                + "        { name: \"Ad block\", icon: \"DOLLAR_SIGN\", value: \"enableAdBlock\" },"
                + "        { name: \"SponsorBlock\", icon: \"MONEY_HAND\", value: \"enableSponsorBlock\" },"
                + "        { name: \"Skip Sponsor Segments\", icon: \"MONEY_HEART\", value: \"enableSponsorBlockSponsor\" },"
                + "        { name: \"Skip Intro Segments\", icon: \"PLAY_CIRCLE\", value: \"enableSponsorBlockIntro\" },"
                + "        { name: \"Skip Outro Segments\", value: \"enableSponsorBlockOutro\" },"
                + "        { name: \"Skip Interaction Reminder Segments\", value: \"enableSponsorBlockInteraction\" },"
                + "        { name: \"Skip Self-Promotion Segments\", value: \"enableSponsorBlockSelfPromo\" },"
                + "        { name: \"Skip Off-Topic Music Segments\", value: \"enableSponsorBlockMusicOfftopic\" },"
                + "        { name: \"Shorts\", icon: \"YOUTUBE_SHORTS_FILL_24\", value: \"enableShorts\" },"
                + "        { name: \"Force Codec AVC1\", icon: \"SETTINGS\", value: \"enableForceAvc1\" }"
                + "    ];"
                + "    const buttons = [];"
                + "    let index = 0;"
                + "    for (const setting of settings) {"
                + "        const currentVal = setting.value ? configRead(setting.value) : null;"
                + "        buttons.push(buttonItem("
                + "            { title: setting.name, subtitle: setting.subtitle },"
                + "            { icon: setting.icon ? setting.icon : \"CHEVRON_DOWN\", secondaryIcon: currentVal === null ? \"CHEVRON_RIGHT\" : (currentVal ? \"CHECK_BOX\" : \"CHECK_BOX_OUTLINE_BLANK\") },"
                + "            currentVal !== null ? [{ setClientSettingEndpoint: { settingDatas: [{ clientSettingEnum: { item: setting.value }, boolValue: !configRead(setting.value) }] } }, { customAction: { action: \"SETTINGS_UPDATE\", parameters: [index] } }] : [{ customAction: { action: \"OPTIONS_SHOW\", parameters: { options: setting.options, selectedIndex: 0, update: false } } }]"
                + "        ));"
                + "        index++;"
                + "    }"
                + "    showModal(\"NoTubeTV Settings\", buttons, parameters && parameters.length > 0 ? parameters[0] : 0, \"tt-settings\", update);"
                + "};"
                + "function resolveCommand(cmd) {"
                + "    for (const key in window._yttv) {"
                + "        if (window._yttv[key] && window._yttv[key].instance && window._yttv[key].instance.resolveCommand) {"
                + "            return window._yttv[key].instance.resolveCommand(cmd);"
                + "        }"
                + "    }"
                + "}"
                + "const NOTUBETV_ACTIONS = [\"SETTINGS_UPDATE\", \"SKIP\"];"
                + "function isNoTubeTVCommand(c) {"
                + "    if (!c) return false;"
                + "    if (c.setClientSettingEndpoint) {"
                + "        const datas = c.setClientSettingEndpoint.settingDatas;"
                + "        return Array.isArray(datas) && datas.length > 0 && datas.every(d => d && d.clientSettingEnum && typeof d.clientSettingEnum.item === \"string\" && Object.prototype.hasOwnProperty.call(defaultConfig, d.clientSettingEnum.item));"
                + "    }"
                + "    if (c.customAction) return NOTUBETV_ACTIONS.includes(c.customAction.action);"
                + "    return false;"
                + "}"
                + "function patchResolveCommand() {"
                + "    let retries = 0;"
                + "    const interval = setInterval(() => {"
                + "        let patched = false;"
                + "        for (const key in window._yttv) {"
                + "            if (window._yttv[key] && window._yttv[key].instance && typeof window._yttv[key].instance.resolveCommand === \"function\") {"
                + "                const instance = window._yttv[key].instance;"
                + "                if (!instance._notubetvPatched) {"
                + "                    const ogResolve = instance.resolveCommand;"
                + "                    instance.resolveCommand = function (cmd, _) {"
                + "                        let commandsToExec = [];"
                + "                        if (cmd && cmd.commandExecutorCommand && Array.isArray(cmd.commandExecutorCommand.commands)) {"
                + "                            commandsToExec = cmd.commandExecutorCommand.commands;"
                + "                        } else if (cmd && (cmd.setClientSettingEndpoint || cmd.customAction)) {"
                + "                            commandsToExec = [cmd];"
                + "                        }"
                + "                        if (commandsToExec.length > 0 && commandsToExec.every(isNoTubeTVCommand)) {"
                + "                            for (const subCmd of commandsToExec) {"
                + "                                if (subCmd.setClientSettingEndpoint) {"
                + "                                    for (const settingData of subCmd.setClientSettingEndpoint.settingDatas) {"
                + "                                        const itemName = settingData.clientSettingEnum.item;"
                + "                                        const valName = Object.keys(settingData).find(k => k.includes(\"Value\"));"
                + "                                        if (valName) {"
                + "                                            const value = valName === \"intValue\" ? Number(settingData[valName]) : settingData[valName];"
                + "                                            configWrite(itemName, value);"
                + "                                            if (itemName === \"enableShorts\") { try { showToast(\"Shorts\", value ? \"Enabled\" : \"Hidden\"); } catch (e) {} }"
                + "                                        }"
                + "                                    }"
                + "                                } else if (subCmd.customAction) {"
                + "                                    customAction(subCmd.customAction.action, subCmd.customAction.parameters);"
                + "                                }"
                + "                            }"
                + "                            return true;"
                + "                        }"
                + "                        return ogResolve.call(this, cmd, _);"
                + "                    };"
                + "                    instance._notubetvPatched = true;"
                + "                    patched = true;"
                + "                }"
                + "            }"
                + "        }"
                + "        if (patched || retries >= 50) { clearInterval(interval); }"
                + "        retries++;"
                + "    }, 200);"
                + "}"
                + "function customAction(action, parameters) {"
                + "    switch (action) {"
                + "        case \"SETTINGS_UPDATE\": modernUI(true, parameters); break;"
                + "        case \"SKIP\": const video = document.querySelector(\"video\"); if (video) { video.currentTime = parameters.time; } resolveCommand({ signalAction: { signal: \"POPUP_BACK\" } }); break;"
                + "    }"
                + "}"
                + "const CODEC_MATCH = { av01: m => m.includes(\"av01\"), vp9: m => m.includes(\"vp9\") || m.includes(\"vp09\") };"
                + "const CODEC_PROBE = { av01: 'video/mp4; codecs=\"av01.0.05M.08\"', vp9: 'video/webm; codecs=\"vp9\"' };"
                + "function isCodecSupported(codec) {"
                + "    try {"
                + "        if (typeof MediaSource === \"undefined\" || typeof MediaSource.isTypeSupported !== \"function\") return true;"
                + "        return MediaSource.isTypeSupported(CODEC_PROBE[codec]);"
                + "    } catch (e) { return true; }"
                + "}"
                + "function formatHeight(f) {"
                + "    if (typeof f.height === \"number\" && f.height > 0) return f.height;"
                + "    const m = /^(\\d+)p/.exec(f.qualityLabel || \"\");"
                + "    return m ? Number(m[1]) : 0;"
                + "}"
                + "function filterFormatsKeepAvc1(formats) {"
                + "    const hasAvc1 = formats.some(f => f.mimeType && f.mimeType.includes(\"avc1\"));"
                + "    if (!hasAvc1) return formats;"
                + "    return formats.filter(f => { const mime = f.mimeType || \"\"; if (mime.startsWith(\"video/\")) return mime.includes(\"avc1\"); return true; });"
                + "}"
                + "function filterFormatsAv01Vp9At720(formats) {"
                + "    const videos = formats.filter(f => (f.mimeType || \"\").startsWith(\"video/\"));"
                + "    if (!videos.length) return formats;"
                + "    const supported = { av01: isCodecSupported(\"av01\"), vp9: isCodecSupported(\"vp9\") };"
                + "    let target = null;"
                + "    for (const codec of [\"av01\", \"vp9\"]) {"
                + "        if (supported[codec] && videos.some(f => CODEC_MATCH[codec](f.mimeType))) { target = codec; break; }"
                + "    }"
                + "    let pool;"
                + "    if (target) {"
                + "        pool = videos.filter(f => CODEC_MATCH[target](f.mimeType));"
                + "    } else {"
                + "        const decodable = videos.filter(f => !(CODEC_MATCH.av01(f.mimeType) && !supported.av01) && !(CODEC_MATCH.vp9(f.mimeType) && !supported.vp9));"
                + "        pool = decodable.length ? decodable : videos;"
                + "    }"
                + "    const heights = [...new Set(pool.map(formatHeight))].filter(h => h > 0).sort((a, b) => a - b);"
                + "    if (!heights.length) return formats;"
                + "    let wanted = heights.includes(720) ? 720 : (heights.filter(h => h < 720).slice(-1)[0] || heights[0]);"
                + "    return formats.filter(f => { if (!(f.mimeType || \"\").startsWith(\"video/\")) return true; return pool.includes(f) && formatHeight(f) === wanted; });"
                + "}"
                + "function filterStreamingDataCodec(streamingData) {"
                + "    if (!streamingData) return;"
                + "    const filterFormats = configRead(\"enableForceAvc1\") ? filterFormatsKeepAvc1 : filterFormatsAv01Vp9At720;"
                + "    for (const key of [\"formats\", \"adaptiveFormats\"]) {"
                + "        if (Array.isArray(streamingData[key])) { streamingData[key] = filterFormats(streamingData[key]); }"
                + "    }"
                + "}"
                + "function processApiResponse(r) {"
                + "    if (!r || typeof r !== \"object\") return r;"
                + "    try {"
                + "        if (configRead(\"enableAdBlock\")) {"
                + "            if (Array.isArray(r.adPlacements)) r.adPlacements = [];"
                + "            if (r.playerAds) r.playerAds = false;"
                + "            if (Array.isArray(r.adSlots)) r.adSlots = [];"
                + "        }"
                + "        filterStreamingDataCodec(r.streamingData);"
                + "    } catch (err) {}"
                + "    return r;"
                + "}"
                + "const origParse = JSON.parse;"
                + "JSON.parse = function () { return processApiResponse(origParse.apply(this, arguments)); };"
                + "const AD_SKIP_TICK_MS = 500;"
                + "setInterval(() => {"
                + "    try {"
                + "        if (!configRead(\"enableAdBlock\")) return;"
                + "        const video = document.querySelector(\"video\");"
                + "        if (!video || video.paused) return;"
                + "        const skipBtn = document.querySelector('[class*=\"skip-ad\" i], [class*=\"SkipAd\"], ytlr-skip-button');"
                + "        if (skipBtn) { skipBtn.click(); }"
                + "    } catch (e) {}"
                + "}, AD_SKIP_TICK_MS);"
                + "function applyShortsHidingCSS() {"
                + "    const id = 'notubetv-hide-shorts';"
                + "    if (!configRead(\"enableShorts\")) {"
                + "        if (!document.getElementById(id)) {"
                + "            const s = document.createElement(\"style\");"
                + "            s.id = id;"
                + "            s.textContent = \"[tvhtml5-shelf-renderer-type='TVHTML5_SHELF_RENDERER_TYPE_SHORTS'], a[href*='/shorts/'] { display: none !important; }\";"
                + "            document.head.appendChild(s);"
                + "        }"
                + "    } else {"
                + "        const el = document.getElementById(id);"
                + "        if (el) el.remove();"
                + "    }"
                + "}"
                + "patchResolveCommand();"
                + "})();";

        // --- 3. Menu Trigger (Từ menu_trigger.js) ---
        final String menuTriggerJS = "javascript:(function () {"
                + "function getSearchBar() {"
                + "    var searchBars = document.querySelectorAll('ytlr-search-text-box');"
                + "    return searchBars[searchBars.length - 1] || null;"
                + "}"
                + "function addMenuButton() {"
                + "    var searchBar = getSearchBar();"
                + "    if (!searchBar) return;"
                + "    var parent = searchBar.parentNode;"
                + "    if (parent.querySelector('button[data-notubetv=\"menu\"]')) return;"
                + "    parent.style.display = 'flex';"
                + "    parent.style.flexDirection = 'row';"
                + "    parent.style.alignItems = 'center';"
                + "    var menuButton = document.createElement('button');"
                + "    menuButton.setAttribute('data-notubetv', 'menu');"
                + "    var svgNS = 'http://www.w3.org/2000/svg';"
                + "    var svg = document.createElementNS(svgNS, 'svg');"
                + "    svg.setAttribute('height', '56px');"
                + "    svg.setAttribute('width', '56px');"
                + "    svg.setAttribute('viewBox', '0 -960 960 960');"
                + "    svg.setAttribute('fill', '#FFFFFF');"
                + "    svg.setAttribute('fill-opacity', '0.8');"
                + "    var path = document.createElementNS(svgNS, 'path');"
                + "    path.setAttribute('d', 'M480-480q0-91 64.5-155.5T700-700q91 0 155.5 64.5T920-480H480ZM260-260q-91 0-155.5-64.5T40-480h440q0 91-64.5 155.5T260-260Zm220-220q-91 0-155.5-64.5T260-700q0-91 64.5-155.5T480-920v440Zm0 440v-440q91 0 155.5 64.5T700-260q0 91-64.5 155.5T480-40Z');"
                + "    svg.appendChild(path);"
                + "    menuButton.appendChild(svg);"
                + "    menuButton.style.display = 'flex';"
                + "    menuButton.style.justifyContent = 'center';"
                + "    menuButton.style.alignItems = 'center';"
                + "    menuButton.style.marginLeft = '54px';"
                + "    menuButton.style.padding = '35px';"
                + "    menuButton.style.background = 'rgba(255, 255, 255, 0.1)';"
                + "    menuButton.style.border = 'none';"
                + "    menuButton.style.borderRadius = '88px';"
                + "    menuButton.style.cursor = 'pointer';"
                + "    parent.insertBefore(menuButton, searchBar.nextSibling);"
                + "}"
                + "addMenuButton();"
                + "document.addEventListener('keydown', function (event) {"
                + "    if (event.key === 'ArrowRight') {"
                + "        var searchBar = getSearchBar();"
                + "        var isFocused = searchBar && searchBar.contains(document.activeElement);"
                + "        if (isFocused) {"
                + "            if (typeof modernUI === 'function') { modernUI(); }"
                + "            var menuBtn = document.querySelector('button[data-notubetv=\"menu\"]');"
                + "            if (menuBtn) { menuBtn.style.background = 'black'; }"
                + "        }"
                + "    }"
                + "});"
                + "var observer = new MutationObserver(function (mutations) {"
                + "    var searchBar = getSearchBar();"
                + "    if (searchBar && document.body.contains(searchBar) && !searchBar.parentNode.querySelector('[data-notubetv=\"menu\"]')) {"
                + "        addMenuButton();"
                + "    }"
                + "});"
                + "observer.observe(document.body, { childList: true, subtree: true });"
                + "})();";

        webView.setWebViewClient(new WebViewClient() {

            @Override @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null) {
                    view.loadUrl(url);
                    return true;
                }
                return false;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Uri u = request.getUrl();
                    view.loadUrl(u != null ? u.toString() : "");
                    return true;
                }
                return false;
            }

            @Override @SuppressWarnings("deprecation")
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                if (isAdEndpoint(url)) {
                    return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
                }
                return super.shouldInterceptRequest(view, url);
            }

            @Nullable
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    String url = request.getUrl().toString();
                    if (isAdEndpoint(url)) {
                        return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript(spoofViewportJS, null);
                view.evaluateJavascript(pageJS, null);
                view.evaluateJavascript(menuTriggerJS, null);
            }

            @Override
            public void onPageCommitVisible(WebView view, String url) {
                view.evaluateJavascript(spoofViewportJS, null);
                view.evaluateJavascript(pageJS, null);
                view.evaluateJavascript(menuTriggerJS, null);
                super.onPageCommitVisible(view, url);
            }

            @Override
            public void onScaleChanged(WebView view, float oldScale, float newScale) {
                view.evaluateJavascript(spoofViewportJS, null);
            }
        });

        webView.setWebChromeClient(new WebChromeClient());
        webView.loadUrl("https://www.youtube.com/tv");
    }

    private static boolean isAdEndpoint(String url) {
        if (url == null) return false;
        return url.contains("googleads.g.doubleclick.net")
                || url.contains("adservice.google.com")
                || url.contains("youtube.com/api/stats/ads")
                || url.contains("doubleclick.net")
                || url.contains("pagead/")
                || url.contains("adserver")
                || url.contains("ytad")
                || (url.contains("manifest.googlevideo.com") && url.contains("ad"))
                || url.matches(".*[?&]adformat=.*");
    }

    // --- 4. Back Button Bridge (Từ back_bridge.js) ---
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView != null) {
            webView.evaluateJavascript(
                    "javascript:(function() {"
                            + "function dispatchKey(key, keyCode, code) {"
                            + "    var downEvent = new KeyboardEvent('keydown', { key: key, keyCode: keyCode, code: code, which: keyCode, bubbles: true, cancelable: true });"
                            + "    var upEvent = new KeyboardEvent('keyup', { key: key, keyCode: keyCode, code: code, which: keyCode, bubbles: true, cancelable: true });"
                            + "    document.dispatchEvent(downEvent);"
                            + "    document.dispatchEvent(upEvent);"
                    + "}"
                            + "var isRootPage = window.location.hash === '#/' || window.location.hash === '' || window.location.hash === '#' || window.location.href.includes('signin');"
                            + "if (!isRootPage) {"
                            + "    window._exitPressCount = 0;"
                            + "    if (window._exitTimer) clearTimeout(window._exitTimer);"
                            + "    dispatchKey('Escape', 27, 'Escape');"
                            + "    return;"
                    + "}"
                            + "if (!window._exitPressCount) window._exitPressCount = 0;"
                            + "window._exitPressCount++;"
                            + "if (window._exitPressCount === 1) {"
                            + "    if (window._exitTimer) clearTimeout(window._exitTimer);"
                            + "    window._exitTimer = setTimeout(function() { window._exitPressCount = 0; }, 1200);"
                            + "    dispatchKey('Escape', 27, 'Escape');"
                    + "} else if (window._exitPressCount >= 2) {"
                            + "    if (window._exitTimer) clearTimeout(window._exitTimer);"
                            + "    window._exitPressCount = 0;"
                            + "    if (typeof ExitBridge !== 'undefined' && ExitBridge.onExitCalled) {"
                            + "        ExitBridge.onExitCalled();"
                            + "    }"
                    + "}"
                            + "})();", null
            );
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}