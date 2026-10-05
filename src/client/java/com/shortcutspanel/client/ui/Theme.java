package com.shortcutspanel.client.ui;

/** 扁平化配色与尺寸常量，全部为 0xAARRGGBB。背景一律中性黑，高亮用青绿，不使用渐变与动画 */
public final class Theme {

    private Theme() {}

    // 背景：中性半透明黑，不偏蓝
    public static final int PANEL = 0xB30B0B0D;
    /** 历史栏曾用更透明的底色显得偏亮，现与 PANEL 一致；常量留着避免大改引用 */
    public static final int PANEL_ALT = PANEL;
    public static final int FIELD = 0xB30D0D0D;
    public static final int CHIP = 0x99161616;
    public static final int CHIP_HOVER = 0xB32A2A2A;
    public static final int BUTTON = 0x99161616;
    public static final int BUTTON_HOVER = 0xB32E2E2E;
    public static final int CAPTURE_SCRIM = 0xCC000000;

    // 前景
    public static final int BORDER = 0x2BFFFFFF;
    public static final int ACCENT = 0xFF2EE8B8;   // 青绿：悬停边框 / 选中态 / 收藏
    public static final int DANGER = 0xFFFF5A5A;   // 红：清空这类不可撤销操作的确认态
    public static final int TEXT = 0xFFF2F4F7;
    public static final int TEXT_DIM = 0xFF9BA2AC;
    public static final int ROW_HOVER = 0x16FFFFFF;
    public static final int ROW_ACTIVE = 0x2A2EE8B8;
    public static final int BUTTON_BOX = 0xCC1C1C1C;

    // 尺寸
    public static final int ROW_HEIGHT = 20;
    public static final int PAD = 12;
    public static final int GAP = 6;
    public static final int INNER_PAD = 6;

    /** 滚动条宽度，窄一点但还抓得住 */
    public static final int SCROLLBAR_THICKNESS = 5;
    /**
     * 行尾要给滚动条让出的宽度。滚动条是叠在内容区上的，条目必须放弃这一条的点击，
     * 否则点击会被条目先消费掉，滚动条就拖不动了
     */
    public static final int SCROLLBAR_RESERVE = 6;

    /** 行尾操作按钮边长 */
    public static final int BUTTON_SIZE = 14;
    /** 搜索行过滤按钮（已绑定 / 已排除）的宽度 */
    public static final int FILTER_BUTTON_WIDTH = 40;
    /**
     * 栏头清空按钮的宽度。高度与过滤按钮一致（ROW_HEIGHT），但宽度只做图标大小的方块——
     * 内容只有一个 ×，拉成过滤按钮那么宽会显得空
     */
    public static final int CLEAR_BUTTON_WIDTH = 20;
    /** 清空按钮里 × 图标的边长（自绘，不用字体的 × 字形，那个偏小） */
    public static final int CLEAR_ICON_SIZE = 8;
    /** × 图标每条斜线的粗细 */
    public static final int CLEAR_ICON_THICKNESS = 2;
    /** 两个操作按钮之间的间距 */
    public static final int BUTTON_GAP = 3;
    /** 行尾为操作按钮预留的宽度（始终占位，悬停时才画出来，避免布局跳动） */
    public static final int ACTION_AREA = BUTTON_SIZE * 2 + BUTTON_GAP;

    /**
     * 三栏按可用宽度分配的百分比。注意总和必须留出两个栏间距的余量（约 3%），
     * 否则 FlowLayout 会横向溢出，整行被挤偏：左栏顶出边距、右栏顶到屏幕边缘。
     */
    public static final int HISTORY_PERCENT = 22;
    public static final int SEARCH_PERCENT = 52;
    public static final int FAVORITES_PERCENT = 22;
}
