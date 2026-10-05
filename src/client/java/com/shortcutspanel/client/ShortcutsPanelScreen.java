package com.shortcutspanel.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.shortcutspanel.client.data.BindEntry;
import com.shortcutspanel.client.data.BindIndex;
import com.shortcutspanel.client.data.BindTrigger;
import com.shortcutspanel.client.data.IdListStore;
import com.shortcutspanel.client.data.Settings;
import com.shortcutspanel.client.ui.BindRowComponent;
import com.shortcutspanel.client.ui.ClearButtonComponent;
import com.shortcutspanel.client.ui.FavoriteChipComponent;
import com.shortcutspanel.client.ui.KeyCaptureScreen;
import com.shortcutspanel.client.ui.EditBoxComponent;
import com.shortcutspanel.client.ui.Theme;
import com.shortcutspanel.client.ui.ToggleButtonComponent;
import com.shortcutspanel.ui.Align;
import com.shortcutspanel.ui.FlowLayout;
import com.shortcutspanel.ui.Insets;
import com.shortcutspanel.ui.LabelComponent;
import com.shortcutspanel.ui.ParentComponent;
import com.shortcutspanel.ui.ScrollContainer;
import com.shortcutspanel.ui.Sizing;
import com.shortcutspanel.ui.Surface;
import com.shortcutspanel.ui.UIComponent;
import com.shortcutspanel.ui.UiScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 快捷键面板：历史栏 | 搜索栏 | 收藏栏，三栏横向排列。
 *
 * <p>UI 用自研的 {@code com.shortcutspanel.ui}，不依赖 owo-lib——
 * 见包内说明：owo 会被别的 mod 注入接口导致类加载冲突。
 */
public final class ShortcutsPanelScreen extends UiScreen {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("shortcuts-panel");

    private static final int MAX_HISTORY = 40;
    /** 按住 Alt 滚动时走这个倍率（VS Code 里 Alt+滚轮也是快速滚动） */
    private static final int ALT_SCROLL_FACTOR = 6;

    private final BindIndex index;
    private final IdListStore favorites;
    private final IdListStore history;
    private final IdListStore excluded;
    private final Settings settings;
    private final List<BindEntry> results = new ArrayList<>();
    private final List<BindRowComponent> rows = new ArrayList<>();
    /** 收藏栏：条目 id -> chip，方便单独增删而不重建整个列表（避免滚动位置丢失） */
    private final Map<String, FavoriteChipComponent> chips = new LinkedHashMap<>();

    private EditBoxComponent searchBox;
    private FlowLayout resultList;
    private ScrollContainer resultScroll;
    private FlowLayout historyList;
    private LabelComponent historyHeader;
    private ScrollContainer historyScroll;
    private FlowLayout favoritesList;
    private LabelComponent favoritesHeader;
    private ScrollContainer favoritesScroll;
    private ToggleButtonComponent boundToggle;
    private ToggleButtonComponent malilibToggle;
    private ToggleButtonComponent excludedToggle;
    /** 收藏栏为空时的提示，添加第一个收藏时要撤掉 */
    private LabelComponent favoritesHint;
    /** 搜索框外层的输入框容器，聚焦时要把描边换成强调色 */
    private FlowLayout searchField;

    /** 收藏栏里正在被拖动的项 */
    private FavoriteChipComponent dragChip;
    /** 临时诊断：布局尺寸只打一次 */
    private boolean loggedLayout;

    /** 重建 UI 后要还原的搜索词（改键界面返回时用） */
    private String retainedQuery = "";
    /**
     * 关面板时记住的结果栏滚动位置，下次开面板还原；-1 表示没有。只在内存里，不落盘。
     * 只在"整个 UI 被重建"时用：{@link #removed} 写入，{@link #init} 交给 pendingScroll、
     * 最后由 {@link #build} 还原。普通的刷新不走它——它不跟着滚动实时更新，
     * 每次刷新都拿它覆盖当前位置的话，列表会被拉回上次记住的地方（看起来就是"回顶"）。
     *
     * <p>只在搜索框为空时有效：有搜索词时结果是筛出来的，而下次开面板搜索框是空的、
     * 结果完全不同，还原那个位置没有意义（该回顶部）。
     */
    private static double retainedScroll = -1;
    /**
     * UI 重建前暂存的滚动位置，{@link #build} 里还原一次；-1 表示没有。
     * 与 retainedScroll 的区别：这个是"同一批结果、UI 重搭一遍"（窗口尺寸变化），
     * 与搜索词无关，必须保留；那个是"跨开关面板"，只有结果集相同才有意义。
     */
    private double pendingScroll = -1;

    private boolean showBound;
    private boolean showMalilib;
    private boolean showExcluded;
    private boolean altHeld;
    private int selected = 0;

    public ShortcutsPanelScreen() {
        super(Component.translatable("screen.shortcuts_panel"));
        this.index = BindIndex.build();
        this.favorites = ShortcutsPanelClient.FAVORITES;
        this.history = ShortcutsPanelClient.HISTORY;
        this.excluded = ShortcutsPanelClient.EXCLUDED;
        this.settings = ShortcutsPanelClient.SETTINGS;
        this.showBound = this.settings.showBound();
        this.showMalilib = this.settings.showMalilib();
        this.showExcluded = this.settings.showExcluded();
        this.index.includeMalilib(this.showMalilib);
        this.index.excluded(this.excluded.ids(), this.showExcluded);
    }

    @Override
    protected void build(FlowLayout root) {
        // 整个界面背景透明，只有各栏面板是半透明黑
        root.padding(Insets.of(Theme.PAD));
        root.gap(Theme.GAP);
        // 三栏宽度合计不到 100%，主轴居中的话左右才对称
        root.mainAlignment(Align.Horizontal.CENTER);

        root.child(this.buildHistoryColumn());
        root.child(this.buildSearchColumn());
        root.child(this.buildFavoritesColumn());

        // 建 UI 时列表内容没变，只是重新搭一遍组件：保留滚动位置
        this.refreshAll(true);
        this.restorePendingScroll();
    }

    /** 还原 UI 重建前暂存的滚动位置（开屏、窗口尺寸变化），只用一次 */
    private void restorePendingScroll() {
        double target = this.pendingScroll;
        this.pendingScroll = -1;
        if (this.resultScroll == null || target < 0) return;

        this.resultScroll.restore(target);
        this.relayout();
    }

    @Override
    protected void init() {
        // 窗口尺寸一变，super.init() 会重建整个 root，ScrollContainer 全是新的，
        // 滚动位置必然丢，所以要在重建之前暂存下来，等 build() 还原。
        // 这里不看搜索词：结果集没换，只是 UI 重搭，有词也要保留。
        // 新开面板时 resultScroll 还是 null，用的就是上次关面板记住的（可能是 -1 = 不还原）。
        this.pendingScroll = this.resultScroll != null ? this.resultScroll.progress() : retainedScroll;

        super.init();
        if (this.searchBox == null) return;

        // 改键界面返回会重新走 init（MC 的 setScreen 会重建），搜索框是新的，
        // 这里把上次输入的词放回去
        if (!this.retainedQuery.isEmpty()) this.searchBox.setValue(this.retainedQuery);
        this.focusSearchBox();
    }

    // ------------------------------------------------------------------ 布局

    private FlowLayout buildHistoryColumn() {
        var column = FlowLayout.vertical(Sizing.fill(Theme.HISTORY_PERCENT), Sizing.fill(100));
        column.gap(Theme.GAP);

        // 表头高度与搜索行一致，三栏的面板顶部才会齐平。
        // 用"占位 + 标题 + 按钮"三列：左右等宽，标题才仍然在整栏居中
        var headerRow = FlowLayout.horizontal(Sizing.fill(100), Sizing.fixed(Theme.ROW_HEIGHT));
        headerRow.verticalAlignment(Align.Vertical.CENTER).gap(4);

        var spacer = new LabelComponent(Component.empty());
        spacer.sizing(Sizing.fixed(Theme.CLEAR_BUTTON_WIDTH), Sizing.fixed(Theme.ROW_HEIGHT));

        this.historyHeader = new LabelComponent(Component.empty())
                .color(Theme.TEXT).shadow(true).align(Align.Horizontal.CENTER);
        this.historyHeader.sizing(Sizing.expand(), Sizing.fixed(Theme.ROW_HEIGHT));

        var clearHistory = new ClearButtonComponent().onConfirm(this::clearHistory);
        clearHistory.sizing(Sizing.fixed(Theme.CLEAR_BUTTON_WIDTH), Sizing.fixed(Theme.ROW_HEIGHT));

        headerRow.child(spacer);
        headerRow.child(this.historyHeader);
        headerRow.child(clearHistory);
        column.child(headerRow);

        this.historyList = FlowLayout.vertical(Sizing.fill(100), Sizing.content());
        this.historyList.gap(2);

        this.historyScroll = new ScrollContainer(Sizing.fill(100), Sizing.fill(100), this.historyList);
        this.historyScroll.scrollbarColor(0x55FFFFFF);

        var panel = FlowLayout.vertical(Sizing.fill(100), Sizing.expand());
        panel.padding(Insets.of(Theme.INNER_PAD));
        panel.surface(Surface.flat(Theme.PANEL_ALT).and(Surface.outline(Theme.BORDER)));
        panel.child(this.historyScroll);
        column.child(panel);

        return column;
    }

    private FlowLayout buildSearchColumn() {
        var column = FlowLayout.vertical(Sizing.fill(Theme.SEARCH_PERCENT), Sizing.fill(100));
        column.gap(Theme.GAP);

        column.child(this.buildSearchRow());

        this.resultList = FlowLayout.vertical(Sizing.fill(100), Sizing.content());
        this.resultList.gap(2);

        this.resultScroll = new ScrollContainer(Sizing.fill(100), Sizing.fill(100), this.resultList);
        this.resultScroll.scrollbarColor(0x55FFFFFF);

        var panel = FlowLayout.vertical(Sizing.fill(100), Sizing.expand());
        panel.padding(Insets.of(Theme.INNER_PAD));
        panel.surface(Surface.flat(Theme.PANEL).and(Surface.outline(Theme.BORDER)));
        panel.child(this.resultScroll);
        column.child(panel);

        return column;
    }

    /** 搜索行：输入框 + 三个过滤开关 */
    private FlowLayout buildSearchRow() {
        var searchRow = FlowLayout.horizontal(Sizing.fill(100), Sizing.fixed(Theme.ROW_HEIGHT));
        searchRow.gap(6).verticalAlignment(Align.Vertical.CENTER);

        // expand()：先排固定宽度的按钮，输入框吃掉剩下的空间
        var field = FlowLayout.horizontal(Sizing.expand(), Sizing.fixed(Theme.ROW_HEIGHT));
        field.surface(Surface.flat(Theme.FIELD).and(Surface.outline(Theme.BORDER)));
        field.padding(Insets.of(0, 0, 8, 0));
        this.searchField = field;

        // 直接包原版 EditBox：输入法、选区、Ctrl+A、双击选词都是原版行为
        this.searchBox = new EditBoxComponent()
                .hint(Component.translatable("shortcuts_panel.search.hint"))
                .onChanged(this::onQueryChanged)
                .onFocusChanged(this::updateSearchFocus);
        this.searchBox.sizing(Sizing.fill(100), Sizing.fill(100));
        field.child(this.searchBox);

        this.boundToggle = new ToggleButtonComponent()
                .onPress(this::toggleBoundFilter)
                .label(Component.empty());
        this.boundToggle.sizing(Sizing.fixed(Theme.FILTER_BUTTON_WIDTH), Sizing.fixed(Theme.ROW_HEIGHT));

        this.malilibToggle = new ToggleButtonComponent()
                .onPress(this::toggleMalilib)
                .label(Component.literal("MaLiLib"));
        this.malilibToggle.sizing(Sizing.fixed(44), Sizing.fixed(Theme.ROW_HEIGHT));

        this.excludedToggle = new ToggleButtonComponent()
                .onPress(this::toggleExcludedFilter)
                .label(Component.translatable("shortcuts_panel.excluded"));
        this.excludedToggle.sizing(Sizing.fixed(Theme.FILTER_BUTTON_WIDTH), Sizing.fixed(Theme.ROW_HEIGHT));

        searchRow.child(field);
        searchRow.child(this.boundToggle);
        searchRow.child(this.malilibToggle);
        searchRow.child(this.excludedToggle);
        return searchRow;
    }

    private FlowLayout buildFavoritesColumn() {
        var column = FlowLayout.vertical(Sizing.fill(Theme.FAVORITES_PERCENT), Sizing.fill(100));
        column.gap(Theme.GAP);

        // 表头高度与搜索行一致，三栏的面板顶部才会齐平
        var headerRow = FlowLayout.horizontal(Sizing.fill(100), Sizing.fixed(Theme.ROW_HEIGHT));
        headerRow.verticalAlignment(Align.Vertical.CENTER).gap(4);

        var spacer = new LabelComponent(Component.empty());
        spacer.sizing(Sizing.fixed(Theme.CLEAR_BUTTON_WIDTH), Sizing.fixed(Theme.ROW_HEIGHT));

        this.favoritesHeader = new LabelComponent(Component.empty())
                .color(Theme.TEXT).shadow(true).align(Align.Horizontal.CENTER);
        this.favoritesHeader.sizing(Sizing.expand(), Sizing.fixed(Theme.ROW_HEIGHT));

        var clearFavorites = new ClearButtonComponent().onConfirm(this::clearFavorites);
        clearFavorites.sizing(Sizing.fixed(Theme.CLEAR_BUTTON_WIDTH), Sizing.fixed(Theme.ROW_HEIGHT));

        headerRow.child(spacer);
        headerRow.child(this.favoritesHeader);
        headerRow.child(clearFavorites);
        column.child(headerRow);

        this.favoritesList = FlowLayout.vertical(Sizing.fill(100), Sizing.content());
        this.favoritesList.gap(4);

        this.favoritesScroll = new ScrollContainer(Sizing.fill(100), Sizing.fill(100), this.favoritesList);
        this.favoritesScroll.scrollbarColor(0x55FFFFFF);

        var panel = FlowLayout.vertical(Sizing.fill(100), Sizing.expand());
        panel.padding(Insets.of(Theme.INNER_PAD));
        panel.surface(Surface.flat(Theme.PANEL).and(Surface.outline(Theme.BORDER)));
        panel.child(this.favoritesScroll);
        column.child(panel);

        return column;
    }

    // ------------------------------------------------------------------ 刷新

    /**
     * 列表内容没变的重建（右键排除、改键返回……）：三栏都保留滚动位置。
     */
    public void refreshAll() {
        this.refreshAll(true);
    }

    /**
     * 重建三栏列表。
     *
     * @param keepScroll true = 保留当前滚动位置；false = 三栏都回到顶部（结果整批换掉时用）
     */
    private void refreshAll(boolean keepScroll) {
        this.refresh(keepScroll);
        this.refreshHistory(keepScroll);
        // 收藏栏也跟着搜索词过滤
        this.rebuildFavorites(keepScroll);
    }

    private void refresh(boolean keepScroll) {
        String query = this.searchBox == null ? "" : this.searchBox.getValue();
        var found = this.index.search(query, this.showBound);

        this.results.clear();
        this.results.addAll(found);
        // 换搜索词/过滤条件后结果整批换掉了，选中项跟着回到第一条，
        // 否则会"滚到顶了但高亮还停在第 N 项"
        if (!keepScroll) this.selected = 0;
        this.selected = Mth.clamp(this.selected, 0, Math.max(0, this.results.size() - 1));

        double progress = keepScroll && this.resultScroll != null ? this.resultScroll.progress() : 0;

        this.rows.clear();
        this.resultList.clearChildren();

        if (this.results.isEmpty()) {
            this.resultList.child(dimLabel("shortcuts_panel.empty"));
        } else {
            for (BindEntry entry : this.results) {
                var row = new BindRowComponent(entry, this, false);
                row.sizing(Sizing.fill(100), Sizing.fixed(Theme.ROW_HEIGHT));
                this.rows.add(row);
                this.resultList.child(row);
            }
        }

        this.updateBoundToggle();
        this.updateMalilibToggle();
        this.updateExcludedToggle();
        this.updateSelection();

        if (this.resultScroll != null) {
            this.relayout();
            this.resultScroll.restore(progress);
        }
    }

    /** 搜索词变了就记下来：改键界面返回、窗口尺寸变化都会重建 UI，不记就丢了 */
    private void onQueryChanged() {
        this.retainedQuery = this.searchBox == null ? "" : this.searchBox.getValue();
        // 结果整批换掉：回到顶部
        this.refreshAll(false);
    }

    /** 关面板时记下滚动位置（内存级，不写配置） */
    @Override
    public void removed() {
        // 只在搜索框为空时记住：有搜索词时结果是筛出来的，下次开面板搜索框是空的、
        // 结果完全不同，还原那个位置只会停在莫名其妙的地方，所以直接作废（-1 = 回顶部）。
        boolean emptyQuery = this.searchBox != null && this.searchBox.getValue().isEmpty();
        retainedScroll = this.resultScroll != null && emptyQuery ? this.resultScroll.progress() : -1;
        super.removed();
    }

    /** 清空最近触发记录 */
    private void clearHistory() {
        var store = ShortcutsPanelClient.HISTORY;
        if (store == null) return;

        for (String id : new ArrayList<>(store.ids())) store.remove(id);
        store.save();
        // 只动了历史栏，结果栏/收藏栏内容没变：位置保留
        this.refreshAll(true);
    }

    /** 清空收藏 */
    private void clearFavorites() {
        var store = ShortcutsPanelClient.FAVORITES;
        if (store == null) return;

        for (String id : new ArrayList<>(store.ids())) store.remove(id);
        store.save();
        this.rebuildFavorites(true);
    }

    private void refreshHistory(boolean keepScroll) {
        String query = this.searchBox == null ? "" : this.searchBox.getValue();
        double progress = keepScroll && this.historyScroll != null ? this.historyScroll.progress() : 0;

        this.historyList.clearChildren();

        int count = 0;
        for (String id : this.history.ids()) {
            if (count >= MAX_HISTORY) break;
            BindEntry entry = this.index.byId(id);
            if (entry == null) continue;
            if (!this.index.matches(entry, query)) continue;

            var row = new BindRowComponent(entry, this, true);
            row.sizing(Sizing.fill(100), Sizing.fixed(Theme.ROW_HEIGHT));
            this.historyList.child(row);
            count++;
        }

        if (count == 0) this.historyList.child(dimLabel("shortcuts_panel.history.empty"));
        this.historyHeader.text(Component.translatable("shortcuts_panel.history", count));
        if (this.historyScroll != null) {
            this.relayout();
            this.historyScroll.restore(progress);
        }
    }

    private void rebuildFavorites(boolean keepScroll) {
        double progress = keepScroll && this.favoritesScroll != null ? this.favoritesScroll.progress() : 0;
        String query = this.searchBox == null ? "" : this.searchBox.getValue();

        this.favoritesList.clearChildren();
        this.chips.clear();
        this.favoritesHint = null;

        int count = 0;
        for (String id : this.favorites.ids()) {
            BindEntry entry = this.index.byId(id);
            if (entry == null) continue;
            if (!this.index.matches(entry, query)) continue;
            this.addChip(entry);
            count++;
        }

        if (count == 0) {
            this.favoritesHint = dimLabel("shortcuts_panel.favorites.empty");
            this.favoritesList.child(this.favoritesHint);
        }
        this.favoritesHeader.text(Component.translatable("shortcuts_panel.favorites", count));
        if (this.favoritesScroll != null) {
            this.relayout();
            this.favoritesScroll.restore(progress);
        }
    }

    private void addChip(BindEntry entry) {
        if (this.chips.containsKey(entry.id())) return;

        // 收藏第一个时把空状态提示撤掉
        if (this.favoritesHint != null) {
            this.favoritesList.removeChild(this.favoritesHint);
            this.favoritesHint = null;
        }

        var chip = new FavoriteChipComponent(entry, this);
        chip.sizing(Sizing.fill(100), Sizing.fixed(Theme.ROW_HEIGHT));
        this.chips.put(entry.id(), chip);
        this.favoritesList.child(chip);
    }

    private void removeChip(BindEntry entry) {
        var chip = this.chips.remove(entry.id());
        if (chip != null) this.favoritesList.removeChild(chip);

        // 收藏清空了要把空状态提示放回来
        if (this.favoritesList.children().isEmpty()) {
            this.favoritesHint = dimLabel("shortcuts_panel.favorites.empty");
            this.favoritesList.child(this.favoritesHint);
        }
    }

    private LabelComponent dimLabel(String translationKey, Object... args) {
        var label = new LabelComponent(Component.translatable(translationKey, args));
        label.color(Theme.TEXT_DIM).shadow(true);
        label.margins(Insets.both(6, 4));
        return label;
    }

    private void updateBoundToggle() {
        if (this.boundToggle == null) return;
        this.boundToggle.label(this.showBound
                ? Component.translatable("shortcuts_panel.bound.shown")
                : Component.translatable("shortcuts_panel.bound.hidden"));
        this.boundToggle.active(this.showBound);
    }

    private void toggleBoundFilter() {
        this.showBound = !this.showBound;
        this.settings.showBound(this.showBound);
        this.settings.save();
        // 过滤条件变了 = 换了一批结果：回到顶部
        this.refreshAll(false);
    }

    private void updateMalilibToggle() {
        if (this.malilibToggle == null) return;
        this.malilibToggle.active(this.showMalilib);
    }

    private void toggleMalilib() {
        this.showMalilib = !this.showMalilib;
        this.settings.showMalilib(this.showMalilib);
        this.settings.save();
        // 索引层的过滤要跟着改，否则要重开面板才生效
        this.index.includeMalilib(this.showMalilib);
        // 过滤条件变了 = 换了一批结果：回到顶部
        this.refreshAll(false);
    }

    private void updateExcludedToggle() {
        if (this.excludedToggle == null) return;
        this.excludedToggle.active(this.showExcluded);
    }

    private void toggleExcludedFilter() {
        this.showExcluded = !this.showExcluded;
        this.settings.showExcluded(this.showExcluded);
        this.settings.save();
        this.index.excluded(this.excluded.ids(), this.showExcluded);
        // 过滤条件变了 = 换了一批结果：回到顶部
        this.refreshAll(false);
    }

    private void updateSelection() {
        for (int i = 0; i < this.rows.size(); i++) {
            this.rows.get(i).setSelected(i == this.selected);
        }
    }

    private void moveSelection(int delta) {
        if (this.rows.isEmpty()) return;
        this.selected = Mth.clamp(this.selected + delta, 0, this.rows.size() - 1);
        this.updateSelection();
        this.revealSelected();
    }

    /** 鼠标划过某行时同步选中项，避免"划到第 5 项、回车却触发第 1 项" */
    public void selectRow(BindRowComponent row) {
        int index = this.rows.indexOf(row);
        if (index < 0 || index == this.selected) return;
        this.selected = index;
        this.updateSelection();
    }

    /** 只有当选中项滚出可视区时才滚动 —— 每按一次方向键都滚会让列表一直跳 */
    private void revealSelected() {
        if (this.resultScroll == null) return;

        var row = this.rows.get(this.selected);
        int viewTop = this.resultScroll.innerY();
        int viewBottom = viewTop + this.resultScroll.innerHeight();
        if (row.y() >= viewTop && row.y() + row.height() <= viewBottom) return;

        double delta = row.y() < viewTop
                ? row.y() - viewTop
                : row.y() + row.height() - viewBottom;

        this.resultScroll.scrollTo(this.resultScroll.scrollOffset() + delta);
        this.relayout();
    }

    // ------------------------------------------------- 供子组件回调的公共方法

    public boolean isFavorite(BindEntry entry) {
        return this.favorites.contains(entry.id());
    }

    public boolean isExcluded(BindEntry entry) {
        return this.excluded.contains(entry.id());
    }

    /** 结果行右键：把条目永久排除（再右键一次放回来） */
    public void toggleExcluded(BindEntry entry) {
        if (!this.excluded.remove(entry.id())) {
            this.excluded.add(entry.id());
        }
        this.excluded.save();
        this.index.excluded(this.excluded.ids(), this.showExcluded);
        // 只是某一行的排除态变了（显示已排除关闭时还会少一行），列表整体没换：保留滚动位置
        this.refreshAll(true);
    }

    public void toggleFavorite(BindEntry entry) {
        double progress = this.favoritesScroll == null ? 0 : this.favoritesScroll.progress();

        if (!this.favorites.remove(entry.id())) {
            this.favorites.add(entry.id());
            this.addChip(entry);
        } else {
            this.removeChip(entry);
        }
        this.favorites.save();

        this.favoritesHeader.text(Component.translatable("shortcuts_panel.favorites", this.chips.size()));
        if (this.favoritesScroll != null) {
            this.relayout();
            this.favoritesScroll.restore(progress);
        }
    }

    public void removeFavorite(BindEntry entry) {
        double progress = this.favoritesScroll == null ? 0 : this.favoritesScroll.progress();

        this.favorites.remove(entry.id());
        this.favorites.save();
        this.removeChip(entry);

        this.favoritesHeader.text(Component.translatable("shortcuts_panel.favorites", this.chips.size()));
        if (this.favoritesScroll != null) {
            this.relayout();
            this.favoritesScroll.restore(progress);
        }
    }

    /** 历史栏右键移除 */
    public void removeHistory(BindEntry entry, BindRowComponent row) {
        double progress = this.historyScroll == null ? 0 : this.historyScroll.progress();

        this.history.remove(entry.id());
        this.history.save();
        this.historyList.removeChild(row);

        if (this.historyList.children().isEmpty()) {
            this.historyList.child(dimLabel("shortcuts_panel.history.empty"));
        }
        this.historyHeader.text(Component.translatable("shortcuts_panel.history", this.history.size()));
        if (this.historyScroll != null) {
            this.relayout();
            this.historyScroll.restore(progress);
        }
    }

    // -------------------------------------------------------------- 拖拽排序

    public void beginDrag(FavoriteChipComponent chip) {
        this.dragChip = chip;
    }

    /** 把正在拖动的项插到鼠标所在的位置 */
    public void dragTo(FavoriteChipComponent chip, int mouseY) {
        var children = this.favoritesList.children();
        int from = children.indexOf(chip);
        if (from < 0) return;

        // 一次只跟相邻的一项交换：鼠标一进入下一项（越过它的上边框）就往下换，
        // 一进入上一项（越过它的下边框）就往上换。跟手，不用等过中线。
        //
        // 会不会来回翻？不会：换完之后被拖项正好落在鼠标所在的那格，
        // 此时"下一项"还在更下方、"上一项"的底边还在更上方，两个条件都不成立。
        // 边界值用 >= 配 < 也正好互补，不会在接缝上抖动。
        int to = from;

        if (from + 1 < children.size()) {
            var next = children.get(from + 1);
            if (mouseY >= next.y()) to = from + 1;
        }
        if (to == from && from > 0) {
            var prev = children.get(from - 1);
            if (mouseY < prev.y() + prev.height()) to = from - 1;
        }
        if (to == from) return;

        int insertAt = to;

        // 收藏栏可能因为搜索词只显示了一部分，可见顺序不等于落盘顺序。
        // 这里先在"可见项"里算出新顺序，再只重排它们在数据层占的槽位。
        var visible = new ArrayList<String>(children.size());
        for (UIComponent other : children) {
            if (other instanceof FavoriteChipComponent otherChip && otherChip != chip) {
                visible.add(otherChip.entryId());
            }
        }
        visible.add(insertAt, chip.entryId());

        this.favorites.reorder(visible);
        this.favorites.save();

        this.favoritesList.removeChild(chip);
        this.favoritesList.child(insertAt, chip);
        this.relayout();
    }

    public void endDrag(FavoriteChipComponent chip) {
        if (this.dragChip == chip) this.dragChip = null;
        this.focusSearchBox();
    }

    public void triggerEntry(BindEntry entry) {
        if (entry == null) return;

        this.history.pushFront(entry.id());
        this.history.trim(MAX_HISTORY);
        this.history.save();

        BindTrigger.request(entry);
    }

    /** 打开按键捕获界面，把该条目绑到玩家按下的键上 */
    public void startKeyCapture(BindEntry entry) {
        var client = Minecraft.getInstance();
        com.shortcutspanel.client.compat.Compat.get().setScreen(client, new KeyCaptureScreen(entry, this));
    }

    // ------------------------------------------------------------------ 输入

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == InputConstants.KEY_LALT || input.key() == InputConstants.KEY_RALT) {
            this.altHeld = true;
            this.syncFastScroll();
        }
        // Esc 一次就关面板。不再做"先清空搜索框、再按才关"：清空有 Ctrl+退格就够了，
        // 两段关闭只会让人以为第一次按没反应。放在前面是为了不让它落进搜索框的输入处理
        if (input.isEscape()) return super.keyPressed(input);
        // 编辑键（退格、光标移动、Home/End、Ctrl+A 全选、Ctrl+C/X/V、双击选词…）
        // 全部交给原版 EditBox，这里不再自己实现一套
        if (this.searchBox != null && this.searchBox.isFocused() && this.searchBox.onKeyPress(input)) {
            return true;
        }
        if (input.isUp()) {
            this.moveSelection(-1);
            return true;
        }
        if (input.isDown()) {
            this.moveSelection(1);
            return true;
        }
        // 回车触发；空格只在搜索框为空时触发，否则留给输入
        if (input.isConfirmation()
                || (input.key() == InputConstants.KEY_SPACE && this.searchBox.getValue().isEmpty())) {
            if (!this.results.isEmpty()) this.triggerEntry(this.results.get(this.selected));
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean keyReleased(KeyEvent input) {
        if (input.key() == InputConstants.KEY_LALT || input.key() == InputConstants.KEY_RALT) {
            this.altHeld = false;
            this.syncFastScroll();
        }
        return super.keyReleased(input);
    }

    /** 调试用：清空搜索词，让结果栏列出全部条目（好验证滚动） */
    public void debugClearSearch() {
        if (this.searchBox != null) this.searchBox.clear();
    }

    /** 调试用：直接设搜索词，好检查文本在框内的位置 */
    public void debugSetSearch(String value) {
        if (this.searchBox != null) this.searchBox.setValue(value);
    }

    /** 调试用：滚到底并打印状态 */
    public void debugScrollToBottom() {
        if (this.resultScroll == null) return;

        LOG.info("scroll[before] {}", this.scrollState());
        this.resultScroll.scrollTo(Double.MAX_VALUE);
        this.relayout();
        LOG.info("scroll[after ] {}", this.scrollState());
    }

    private String scrollState() {
        return "rows=" + this.resultList.children().size()
                + " viewport=" + this.resultScroll.height()
                + " content=" + this.resultList.height()
                + " max=" + this.resultScroll.maxScroll()
                + " offset=" + Math.round(this.resultScroll.scrollOffset());
    }

    /** 临时：把布局尺寸打到日志，用来定位宽度异常 */
    @Override
    public void relayout() {
        super.relayout();
        if (this.loggedLayout || this.root == null || this.root.width() == 0) return;
        this.loggedLayout = true;

        LOG.info("layout: screen={}x{} root={}x{} inner={}x{}",
                this.width, this.height, this.root.width(), this.root.height(),
                this.root.innerWidth(), this.root.innerHeight());
        for (var child : this.root.children()) {
            LOG.info("  child {}x{} @ ({},{}) sizing={}/{} (v={})",
                    child.width(), child.height(), child.x(), child.y(),
                    child.horizontalSizing().mode(), child.horizontalSizing().value(),
                    child.verticalSizing().mode());
            if (!(child instanceof ParentComponent column) || column.children().isEmpty()) continue;

            var first = column.children().get(0);
            LOG.info("    first {}x{} @ ({},{})", first.width(), first.height(), first.x(), first.y());
            if (first instanceof ParentComponent row && !row.children().isEmpty()) {
                var leaf = row.children().get(0);
                LOG.info("      leaf {}x{} @ ({},{})", leaf.width(), leaf.height(), leaf.x(), leaf.y());
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        // 单机游戏里打开面板不暂停世界
        return false;
    }

    /** Alt 状态变化时同步给三个滚动容器（Screen.tick 在 26.x 里不一定被调用） */
    private void syncFastScroll() {
        for (var scroll : new ScrollContainer[]{this.historyScroll, this.resultScroll, this.favoritesScroll}) {
            if (scroll != null) scroll.fastScroll(this.altHeld, ALT_SCROLL_FACTOR);
        }
    }

    /** 聚焦态：输入框描边换成强调色，键盘用户才知道焦点在哪 */
    private void updateSearchFocus(boolean focused) {
        if (this.searchField == null) return;
        this.searchField.surface(Surface.flat(Theme.FIELD)
                .and(Surface.outline(focused ? Theme.ACCENT : Theme.BORDER)));
    }

    private void focusSearchBox() {
        if (this.searchBox != null) this.focus(this.searchBox);
    }

    /**
     * 面板里只有搜索框能打字，所以即使它当前没聚焦（比如刚点过列表），
     * 敲下的字符也应该回到它那里，而不是什么都没发生。
     */
    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent input) {
        if (this.searchBox != null && !this.searchBox.isFocused()) this.focusSearchBox();
        return super.charTyped(input);
    }

    /** 点搜索框所在这一行的任意位置都聚焦到输入框 */
    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
        boolean handled = super.mouseClicked(event, doubled);
        if (!handled && this.searchField != null && this.searchField.isInBoundingBox(event.x(), event.y())) {
            this.focusSearchBox();
            return true;
        }
        return handled;
    }
}
