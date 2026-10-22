package com.shortcutspanel.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.shortcutspanel.client.data.BindEntry;
import com.shortcutspanel.client.data.BindIndex;
import com.shortcutspanel.client.data.BindTrigger;
import com.shortcutspanel.client.data.IdListStore;
import com.shortcutspanel.client.data.Settings;
import com.shortcutspanel.client.ui.BindRowComponent;
import com.shortcutspanel.client.ui.ClearButtonComponent;
import com.shortcutspanel.client.ui.EditBoxComponent;
import com.shortcutspanel.client.ui.FavoriteChipComponent;
import com.shortcutspanel.client.ui.KeyCaptureScreen;
import com.shortcutspanel.client.ui.Theme;
import com.shortcutspanel.client.ui.ToggleButtonComponent;
import com.shortcutspanel.client.uikit.Align;
import com.shortcutspanel.client.uikit.FlowLayout;
import com.shortcutspanel.client.uikit.Insets;
import com.shortcutspanel.client.uikit.LabelComponent;
import com.shortcutspanel.client.uikit.ParentComponent;
import com.shortcutspanel.client.uikit.ScrollContainer;
import com.shortcutspanel.client.uikit.Sizing;
import com.shortcutspanel.client.uikit.Surface;
import com.shortcutspanel.client.uikit.UIComponent;
import com.shortcutspanel.client.uikit.UiScreen;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class ShortcutsPanelScreen extends UiScreen {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("shortcutspanel");

    private static final int MAX_HISTORY = 40;

    private static final int ALT_SCROLL_FACTOR = 6;

    private final BindIndex index;
    private final IdListStore favorites;
    private final IdListStore history;
    private final IdListStore excluded;
    private final Settings settings;
    private final List<BindEntry> results = new ArrayList<>();
    private final List<BindRowComponent> rows = new ArrayList<>();

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

    private LabelComponent favoritesHint;

    private FlowLayout searchField;

    private FavoriteChipComponent dragChip;

    private boolean loggedLayout;

    private static String retainedQuery = "";

    private static double retainedScroll = -1;

    private boolean selectAllOnBuild;

    private boolean restoringQuery;

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
        root.padding(Insets.of(Theme.PAD));
        root.gap(Theme.GAP);
        root.mainAlignment(Align.Horizontal.CENTER);

        root.child(this.buildHistoryColumn());
        root.child(this.buildSearchColumn());
        root.child(this.buildFavoritesColumn());

        this.relayout();

        if (!retainedQuery.isEmpty()) {
            this.restoringQuery = true;
            this.searchBox.setValue(retainedQuery);
            this.restoringQuery = false;
            if (this.selectAllOnBuild) this.searchBox.selectAll();
        }
        this.selectAllOnBuild = false;

        this.refreshAll(true);
        this.restorePendingScroll();
    }

    private void restorePendingScroll() {
        double target = this.pendingScroll;
        this.pendingScroll = -1;
        if (this.resultScroll == null || target < 0) return;

        this.resultScroll.restore(target);
        this.relayout();
    }

    @Override
    protected void init() {
        boolean freshOpen = this.resultScroll == null;
        this.pendingScroll = freshOpen ? retainedScroll : this.resultScroll.progress();
        this.selectAllOnBuild = freshOpen;

        super.init();
        if (this.searchBox == null) return;

        if (!retainedQuery.isEmpty() && !retainedQuery.equals(this.searchBox.getValue())) {
            this.searchBox.setValue(retainedQuery);
        }
        this.focusSearchBox();
    }

    private FlowLayout buildHistoryColumn() {
        var column = FlowLayout.vertical(Sizing.fill(Theme.HISTORY_PERCENT), Sizing.fill(100));
        column.gap(Theme.GAP);

        var headerRow = FlowLayout.horizontal(Sizing.fill(100), Sizing.fixed(Theme.ROW_HEIGHT));
        headerRow.verticalAlignment(Align.Vertical.CENTER).gap(4);

        var spacer = new LabelComponent(Component.empty());
        spacer.sizing(Sizing.fixed(Theme.CLEAR_BUTTON_WIDTH), Sizing.fixed(Theme.ROW_HEIGHT));

        this.historyHeader =
                new LabelComponent(Component.empty())
                        .color(Theme.TEXT)
                        .shadow(true)
                        .align(Align.Horizontal.CENTER);
        this.historyHeader.sizing(Sizing.expand(), Sizing.fixed(Theme.ROW_HEIGHT));

        var clearHistory = new ClearButtonComponent().onConfirm(this::clearHistory);
        clearHistory.sizing(Sizing.fixed(Theme.CLEAR_BUTTON_WIDTH), Sizing.fixed(Theme.ROW_HEIGHT));

        headerRow.child(spacer);
        headerRow.child(this.historyHeader);
        headerRow.child(clearHistory);
        column.child(headerRow);

        this.historyList = FlowLayout.vertical(Sizing.fill(100), Sizing.content());
        this.historyList.gap(2);

        this.historyScroll =
                new ScrollContainer(Sizing.fill(100), Sizing.fill(100), this.historyList);
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

        this.resultScroll =
                new ScrollContainer(Sizing.fill(100), Sizing.fill(100), this.resultList);
        this.resultScroll.scrollbarColor(0x55FFFFFF);

        var panel = FlowLayout.vertical(Sizing.fill(100), Sizing.expand());
        panel.padding(Insets.of(Theme.INNER_PAD));
        panel.surface(Surface.flat(Theme.PANEL).and(Surface.outline(Theme.BORDER)));
        panel.child(this.resultScroll);
        column.child(panel);

        return column;
    }

    private FlowLayout buildSearchRow() {
        var searchRow = FlowLayout.horizontal(Sizing.fill(100), Sizing.fixed(Theme.ROW_HEIGHT));
        searchRow.gap(6).verticalAlignment(Align.Vertical.CENTER);

        var field = FlowLayout.horizontal(Sizing.expand(), Sizing.fixed(Theme.ROW_HEIGHT));
        field.surface(Surface.flat(Theme.FIELD).and(Surface.outline(Theme.BORDER)));
        field.padding(Insets.of(0, 0, 8, 0));
        this.searchField = field;

        this.searchBox =
                new EditBoxComponent()
                        .hint(Component.translatable("shortcuts_panel.search.hint"))
                        .onChanged(this::onQueryChanged)
                        .onFocusChanged(this::updateSearchFocus);
        this.searchBox.sizing(Sizing.fill(100), Sizing.fill(100));
        field.child(this.searchBox);

        this.boundToggle =
                new ToggleButtonComponent()
                        .onPress(this::toggleBoundFilter)
                        .label(Component.empty());
        this.boundToggle.sizing(
                Sizing.fixed(Theme.FILTER_BUTTON_WIDTH), Sizing.fixed(Theme.ROW_HEIGHT));

        this.malilibToggle =
                new ToggleButtonComponent()
                        .onPress(this::toggleMalilib)
                        .label(Component.literal("MaLiLib"));
        this.malilibToggle.sizing(Sizing.fixed(44), Sizing.fixed(Theme.ROW_HEIGHT));

        this.excludedToggle =
                new ToggleButtonComponent()
                        .onPress(this::toggleExcludedFilter)
                        .label(Component.translatable("shortcuts_panel.excluded"));
        this.excludedToggle.sizing(
                Sizing.fixed(Theme.FILTER_BUTTON_WIDTH), Sizing.fixed(Theme.ROW_HEIGHT));

        searchRow.child(field);
        searchRow.child(this.boundToggle);
        searchRow.child(this.malilibToggle);
        searchRow.child(this.excludedToggle);
        return searchRow;
    }

    private FlowLayout buildFavoritesColumn() {
        var column = FlowLayout.vertical(Sizing.fill(Theme.FAVORITES_PERCENT), Sizing.fill(100));
        column.gap(Theme.GAP);

        var headerRow = FlowLayout.horizontal(Sizing.fill(100), Sizing.fixed(Theme.ROW_HEIGHT));
        headerRow.verticalAlignment(Align.Vertical.CENTER).gap(4);

        var spacer = new LabelComponent(Component.empty());
        spacer.sizing(Sizing.fixed(Theme.CLEAR_BUTTON_WIDTH), Sizing.fixed(Theme.ROW_HEIGHT));

        this.favoritesHeader =
                new LabelComponent(Component.empty())
                        .color(Theme.TEXT)
                        .shadow(true)
                        .align(Align.Horizontal.CENTER);
        this.favoritesHeader.sizing(Sizing.expand(), Sizing.fixed(Theme.ROW_HEIGHT));

        var clearFavorites = new ClearButtonComponent().onConfirm(this::clearFavorites);
        clearFavorites.sizing(
                Sizing.fixed(Theme.CLEAR_BUTTON_WIDTH), Sizing.fixed(Theme.ROW_HEIGHT));

        headerRow.child(spacer);
        headerRow.child(this.favoritesHeader);
        headerRow.child(clearFavorites);
        column.child(headerRow);

        this.favoritesList = FlowLayout.vertical(Sizing.fill(100), Sizing.content());
        this.favoritesList.gap(4);

        this.favoritesScroll =
                new ScrollContainer(Sizing.fill(100), Sizing.fill(100), this.favoritesList);
        this.favoritesScroll.scrollbarColor(0x55FFFFFF);

        var panel = FlowLayout.vertical(Sizing.fill(100), Sizing.expand());
        panel.padding(Insets.of(Theme.INNER_PAD));
        panel.surface(Surface.flat(Theme.PANEL).and(Surface.outline(Theme.BORDER)));
        panel.child(this.favoritesScroll);
        column.child(panel);

        return column;
    }

    public void refreshAll() {
        this.refreshAll(true);
    }

    private void refreshAll(boolean keepScroll) {
        this.refresh(keepScroll);
        this.refreshHistory(keepScroll);
        this.rebuildFavorites(keepScroll);
    }

    private void refresh(boolean keepScroll) {
        String query = this.searchBox == null ? "" : this.searchBox.getValue();
        var found = this.index.search(query, this.showBound);

        this.results.clear();
        this.results.addAll(found);
        if (!keepScroll) this.selected = 0;
        this.selected = Mth.clamp(this.selected, 0, Math.max(0, this.results.size() - 1));

        double progress =
                keepScroll && this.resultScroll != null ? this.resultScroll.progress() : 0;

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

    private void onQueryChanged() {
        retainedQuery = this.searchBox == null ? "" : this.searchBox.getValue();
        if (this.restoringQuery) return;
        this.refreshAll(false);
    }

    @Override
    public void removed() {
        retainedScroll = this.resultScroll != null ? this.resultScroll.progress() : -1;
        if (this.searchBox != null) retainedQuery = this.searchBox.getValue();
        super.removed();
    }

    private void clearHistory() {
        var store = ShortcutsPanelClient.HISTORY;
        if (store == null) return;

        for (String id : new ArrayList<>(store.ids())) store.remove(id);
        store.save();
        this.refreshAll(true);
    }

    private void clearFavorites() {
        var store = ShortcutsPanelClient.FAVORITES;
        if (store == null) return;

        for (String id : new ArrayList<>(store.ids())) store.remove(id);
        store.save();
        this.rebuildFavorites(true);
    }

    private void refreshHistory(boolean keepScroll) {
        String query = this.searchBox == null ? "" : this.searchBox.getValue();
        double progress =
                keepScroll && this.historyScroll != null ? this.historyScroll.progress() : 0;

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
        double progress =
                keepScroll && this.favoritesScroll != null ? this.favoritesScroll.progress() : 0;
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
        this.boundToggle.label(
                this.showBound
                        ? Component.translatable("shortcuts_panel.bound.shown")
                        : Component.translatable("shortcuts_panel.bound.hidden"));
        this.boundToggle.active(this.showBound);
    }

    private void toggleBoundFilter() {
        this.showBound = !this.showBound;
        this.settings.showBound(this.showBound);
        this.settings.save();
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
        this.index.includeMalilib(this.showMalilib);
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

    public void selectRow(BindRowComponent row) {
        int index = this.rows.indexOf(row);
        if (index < 0 || index == this.selected) return;
        this.selected = index;
        this.updateSelection();
    }

    private void revealSelected() {
        if (this.resultScroll == null) return;

        var row = this.rows.get(this.selected);
        int viewTop = this.resultScroll.innerY();
        int viewBottom = viewTop + this.resultScroll.innerHeight();
        if (row.y() >= viewTop && row.y() + row.height() <= viewBottom) return;

        double delta = row.y() < viewTop ? row.y() - viewTop : row.y() + row.height() - viewBottom;

        this.resultScroll.scrollTo(this.resultScroll.scrollOffset() + delta);
        this.relayout();
    }

    public boolean isFavorite(BindEntry entry) {
        return this.favorites.contains(entry.id());
    }

    public boolean isExcluded(BindEntry entry) {
        return this.excluded.contains(entry.id());
    }

    public void toggleExcluded(BindEntry entry) {
        if (!this.excluded.remove(entry.id())) {
            this.excluded.add(entry.id());
        }
        this.excluded.save();
        this.index.excluded(this.excluded.ids(), this.showExcluded);
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

        this.favoritesHeader.text(
                Component.translatable("shortcuts_panel.favorites", this.chips.size()));
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

        this.favoritesHeader.text(
                Component.translatable("shortcuts_panel.favorites", this.chips.size()));
        if (this.favoritesScroll != null) {
            this.relayout();
            this.favoritesScroll.restore(progress);
        }
    }

    public void removeHistory(BindEntry entry, BindRowComponent row) {
        double progress = this.historyScroll == null ? 0 : this.historyScroll.progress();

        this.history.remove(entry.id());
        this.history.save();
        this.historyList.removeChild(row);

        if (this.historyList.children().isEmpty()) {
            this.historyList.child(dimLabel("shortcuts_panel.history.empty"));
        }
        this.historyHeader.text(
                Component.translatable("shortcuts_panel.history", this.history.size()));
        if (this.historyScroll != null) {
            this.relayout();
            this.historyScroll.restore(progress);
        }
    }

    public void beginDrag(FavoriteChipComponent chip) {
        this.dragChip = chip;
    }

    public void dragTo(FavoriteChipComponent chip, int mouseY) {
        var children = this.favoritesList.children();
        int from = children.indexOf(chip);
        if (from < 0) return;

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

    public void startKeyCapture(BindEntry entry) {
        if (entry == null || entry.isMalilib()) return;

        var client = Minecraft.getInstance();
        com.shortcutspanel.client.compat.Compat.get()
                .setScreen(client, new KeyCaptureScreen(entry, this));
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == InputConstants.KEY_LALT || input.key() == InputConstants.KEY_RALT) {
            this.altHeld = true;
            this.syncFastScroll();
        }
        if (input.isEscape()) return super.keyPressed(input);
        if (this.searchBox != null
                && this.searchBox.isFocused()
                && this.searchBox.onKeyPress(input)) {
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
        if (input.isConfirmation()
                || (input.key() == InputConstants.KEY_SPACE
                        && this.searchBox.getValue().isEmpty())) {
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

    public void debugClearSearch() {
        if (this.searchBox != null) this.searchBox.clear();
    }

    public void debugSetSearch(String value) {
        if (this.searchBox != null) this.searchBox.setValue(value);
    }

    public void debugScrollToBottom() {
        if (this.resultScroll == null) return;

        LOG.info("scroll[before] {}", this.scrollState());
        this.resultScroll.scrollTo(Double.MAX_VALUE);
        this.relayout();
        LOG.info("scroll[after ] {}", this.scrollState());
    }

    private String scrollState() {
        return "rows="
                + this.resultList.children().size()
                + " viewport="
                + this.resultScroll.height()
                + " content="
                + this.resultList.height()
                + " max="
                + this.resultScroll.maxScroll()
                + " offset="
                + Math.round(this.resultScroll.scrollOffset());
    }

    @Override
    public void relayout() {
        super.relayout();
        if (this.loggedLayout || this.root == null || this.root.width() == 0) return;
        this.loggedLayout = true;

        LOG.info(
                "layout: screen={}x{} root={}x{} inner={}x{}",
                this.width,
                this.height,
                this.root.width(),
                this.root.height(),
                this.root.innerWidth(),
                this.root.innerHeight());
        for (var child : this.root.children()) {
            LOG.info(
                    "  child {}x{} @ ({},{}) sizing={}/{} (v={})",
                    child.width(),
                    child.height(),
                    child.x(),
                    child.y(),
                    child.horizontalSizing().mode(),
                    child.horizontalSizing().value(),
                    child.verticalSizing().mode());
            if (!(child instanceof ParentComponent column) || column.children().isEmpty()) continue;

            var first = column.children().get(0);
            LOG.info(
                    "    first {}x{} @ ({},{})",
                    first.width(),
                    first.height(),
                    first.x(),
                    first.y());
            if (first instanceof ParentComponent row && !row.children().isEmpty()) {
                var leaf = row.children().get(0);
                LOG.info(
                        "      leaf {}x{} @ ({},{})",
                        leaf.width(),
                        leaf.height(),
                        leaf.x(),
                        leaf.y());
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void syncFastScroll() {
        for (var scroll :
                new ScrollContainer[] {
                    this.historyScroll, this.resultScroll, this.favoritesScroll
                }) {
            if (scroll != null) scroll.fastScroll(this.altHeld, ALT_SCROLL_FACTOR);
        }
    }

    private void updateSearchFocus(boolean focused) {
        if (this.searchField == null) return;
        this.searchField.surface(
                Surface.flat(Theme.FIELD)
                        .and(Surface.outline(focused ? Theme.ACCENT : Theme.BORDER)));
    }

    private void focusSearchBox() {
        if (this.searchBox != null) this.focus(this.searchBox);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent input) {
        if (this.searchBox != null && !this.searchBox.isFocused()) this.focusSearchBox();
        return super.charTyped(input);
    }

    @Override
    public boolean mouseClicked(
            net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
        boolean handled = super.mouseClicked(event, doubled);
        if (!handled
                && this.searchField != null
                && this.searchField.isInBoundingBox(event.x(), event.y())) {
            this.focusSearchBox();
            return true;
        }
        return handled;
    }
}
