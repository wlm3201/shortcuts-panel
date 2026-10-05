package com.shortcutspanel.ui;

import java.util.ArrayList;
import java.util.List;

/** 单向流式布局：按添加顺序竖排或横排 */
public final class FlowLayout extends ParentComponent {

    public enum Direction { VERTICAL, HORIZONTAL }

    private final Direction direction;
    private int gap;
    private Align.Horizontal horizontalAlignment = Align.Horizontal.LEFT;
    private Align.Vertical verticalAlignment = Align.Vertical.TOP;
    /** 主轴上的整体对齐（横排看水平，竖排看垂直） */
    private Align.Horizontal mainHorizontal = Align.Horizontal.LEFT;
    private Align.Vertical mainVertical = Align.Vertical.TOP;

    public FlowLayout(Direction direction, Sizing horizontal, Sizing vertical) {
        this.direction = direction;
        this.sizing(horizontal, vertical);
    }

    public static FlowLayout vertical(Sizing horizontal, Sizing vertical) {
        return new FlowLayout(Direction.VERTICAL, horizontal, vertical);
    }

    public static FlowLayout horizontal(Sizing horizontal, Sizing vertical) {
        return new FlowLayout(Direction.HORIZONTAL, horizontal, vertical);
    }

    public FlowLayout gap(int gap) {
        this.gap = gap;
        return this;
    }

    public FlowLayout horizontalAlignment(Align.Horizontal alignment) {
        this.horizontalAlignment = alignment;
        return this;
    }

    public FlowLayout verticalAlignment(Align.Vertical alignment) {
        this.verticalAlignment = alignment;
        return this;
    }

    /**
     * 主轴整体对齐。注意它和 {@link #horizontalAlignment} / {@link #verticalAlignment} 不同——
     * 后两者只管交叉轴：
     * <ul>
     *   <li>横排容器：主轴是水平 → 用本方法的 {@link Align.Horizontal} 版本</li>
     *   <li>竖排容器：主轴是垂直 → 用本方法的 {@link Align.Vertical} 版本</li>
     * </ul>
     */
    public FlowLayout mainAlignment(Align.Horizontal alignment) {
        this.mainHorizontal = alignment;
        return this;
    }

    /** 竖排容器的主轴（垂直）对齐，见 {@link #mainAlignment(Align.Horizontal)} */
    public FlowLayout mainAlignment(Align.Vertical alignment) {
        this.mainVertical = alignment;
        return this;
    }

    /** 主轴方向占用的总长度（含间距与子项外边距） */
    private int totalMainLength(boolean vertical) {
        int total = 0;
        for (UIComponent child : this.children) {
            total += vertical ? child.height() : child.width();
            total += vertical
                    ? child.margins().top() + child.margins().bottom()
                    : child.margins().left() + child.margins().right();
        }
        return total + this.gap * Math.max(0, this.children.size() - 1);
    }

    @Override
    public void inflate(int availableWidth, int availableHeight) {
        super.inflate(availableWidth, availableHeight);

        // 先让子项按自己的策略算出尺寸，容器若是 content 才有依据
        int innerWidth = this.innerWidth();
        int innerHeight = this.innerHeight();

        for (UIComponent child : this.children) {
            child.inflate(innerWidth, innerHeight);
        }

        if (this.horizontalSizing().mode() == Sizing.Mode.CONTENT) {
            this.width = this.contentWidth() + this.padding().left() + this.padding().right();
        }
        if (this.verticalSizing().mode() == Sizing.Mode.CONTENT) {
            this.height = this.contentHeight() + this.padding().top() + this.padding().bottom();
        }
    }

    @Override
    protected int contentWidth() {
        if (this.direction == Direction.HORIZONTAL) {
            int total = 0;
            for (UIComponent child : this.children) {
                total += child.width() + child.margins().left() + child.margins().right();
            }
            return total + this.gap * Math.max(0, this.children.size() - 1);
        }

        int max = 0;
        for (UIComponent child : this.children) {
            max = Math.max(max, child.width() + child.margins().left() + child.margins().right());
        }
        return max;
    }

    @Override
    protected int contentHeight() {
        if (this.direction == Direction.VERTICAL) {
            int total = 0;
            for (UIComponent child : this.children) {
                total += child.height() + child.margins().top() + child.margins().bottom();
            }
            return total + this.gap * Math.max(0, this.children.size() - 1);
        }

        int max = 0;
        for (UIComponent child : this.children) {
            max = Math.max(max, child.height() + child.margins().top() + child.margins().bottom());
        }
        return max;
    }

    @Override
    protected void layoutChildren() {
        if (this.children.isEmpty()) return;

        int innerWidth = this.innerWidth();
        int innerHeight = this.innerHeight();
        int innerX = this.innerX();
        int innerY = this.innerY();

        // 先给固定 / 内容 / 百分比的子项定尺寸，剩下的是 expand 的
        int usedMain = 0;
        int expandCount = 0;
        List<UIComponent> expandable = new ArrayList<>();

        for (UIComponent child : this.children) {
            Sizing main = this.direction == Direction.VERTICAL
                    ? child.verticalSizing()
                    : child.horizontalSizing();

            if (main.mode() == Sizing.Mode.EXPAND) {
                expandCount++;
                expandable.add(child);
                continue;
            }

            int mainSpace = this.direction == Direction.VERTICAL ? innerHeight : innerWidth;
            child.inflate(innerWidth, innerHeight);
            int size = this.direction == Direction.VERTICAL ? child.height() : child.width();
            usedMain += size + (this.direction == Direction.VERTICAL
                    ? child.margins().top() + child.margins().bottom()
                    : child.margins().left() + child.margins().right());
            if (main.mode() == Sizing.Mode.FIXED && main.value() > mainSpace) {
                usedMain -= size - mainSpace;
            }
        }

        int gaps = this.gap * Math.max(0, this.children.size() - 1);
        int mainSpace = this.direction == Direction.VERTICAL ? innerHeight : innerWidth;
        int remaining = Math.max(0, mainSpace - usedMain - gaps);

        for (UIComponent child : expandable) {
            int share = expandCount == 0 ? remaining : remaining / expandCount;
            if (this.direction == Direction.VERTICAL) {
                child.inflate(innerWidth, share);
            } else {
                child.inflate(share, innerHeight);
            }
        }

        boolean vertical = this.direction == Direction.VERTICAL;

        // 交叉轴按父容器可用空间重新解析，主轴尺寸保持上面算好的值不动。
        // 两个坑都在这里：
        //  1. 不能拿子项自己的尺寸当可用空间——fill(22%) 会被二次应用（616→136→30）；
        //  2. 不能用 inflate 整体重算——expand 子项会被重置成整行宽，
        //     把同一行的其它固定宽度子项挤出容器。
        for (UIComponent child : this.children) {
            if (vertical) {
                child.setSize(UIComponent.resolve(child.horizontalSizing(), innerWidth,
                        child.contentWidthForLayout()), child.height());
            } else {
                child.setSize(child.width(), UIComponent.resolve(child.verticalSizing(), innerHeight,
                        child.contentHeightForLayout()));
            }
        }

        int cursor = vertical ? innerY : innerX;

        // 主轴整体对齐：先按整行/整列的总长度算出起点偏移
        int mainExtent = vertical ? innerHeight : innerWidth;
        int slack = Math.max(0, mainExtent - this.totalMainLength(vertical));
        cursor += vertical
                ? switch (this.mainVertical) {
                    case TOP -> 0;
                    case CENTER -> slack / 2;
                    case BOTTOM -> slack;
                }
                : switch (this.mainHorizontal) {
                    case LEFT -> 0;
                    case CENTER -> slack / 2;
                    case RIGHT -> slack;
                };

        for (UIComponent child : this.children) {
            cursor += vertical ? child.margins().top() : child.margins().left();

            if (vertical) {
                int offset = switch (this.horizontalAlignment) {
                    case LEFT -> 0;
                    case CENTER -> (innerWidth - child.width()) / 2;
                    case RIGHT -> innerWidth - child.width();
                };
                child.moveTo(innerX + offset, cursor);
                cursor += child.height() + child.margins().bottom() + this.gap;
            } else {
                int offset = switch (this.verticalAlignment) {
                    case TOP -> 0;
                    case CENTER -> (innerHeight - child.height()) / 2;
                    case BOTTOM -> innerHeight - child.height();
                };
                child.moveTo(cursor, innerY + offset);
                cursor += child.width() + child.margins().right() + this.gap;
            }
            child.layout();
        }
    }
}
