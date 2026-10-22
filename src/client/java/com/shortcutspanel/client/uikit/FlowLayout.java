package com.shortcutspanel.client.uikit;

import java.util.ArrayList;
import java.util.List;

public final class FlowLayout extends ParentComponent {

    public enum Direction {
        VERTICAL,
        HORIZONTAL
    }

    private final Direction direction;
    private int gap;
    private Align.Horizontal horizontalAlignment = Align.Horizontal.LEFT;
    private Align.Vertical verticalAlignment = Align.Vertical.TOP;

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

    public FlowLayout mainAlignment(Align.Horizontal alignment) {
        this.mainHorizontal = alignment;
        return this;
    }

    public FlowLayout mainAlignment(Align.Vertical alignment) {
        this.mainVertical = alignment;
        return this;
    }

    private int totalMainLength(boolean vertical) {
        int total = 0;
        for (UIComponent child : this.children) {
            total += vertical ? child.height() : child.width();
            total +=
                    vertical
                            ? child.margins().top() + child.margins().bottom()
                            : child.margins().left() + child.margins().right();
        }
        return total + this.gap * Math.max(0, this.children.size() - 1);
    }

    @Override
    public void inflate(int availableWidth, int availableHeight) {
        super.inflate(availableWidth, availableHeight);

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

        int usedMain = 0;
        int expandCount = 0;
        List<UIComponent> expandable = new ArrayList<>();

        for (UIComponent child : this.children) {
            Sizing main =
                    this.direction == Direction.VERTICAL
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
            usedMain +=
                    size
                            + (this.direction == Direction.VERTICAL
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

        for (UIComponent child : this.children) {
            if (vertical) {
                child.setSize(
                        UIComponent.resolve(
                                child.horizontalSizing(),
                                innerWidth,
                                child.contentWidthForLayout()),
                        child.height());
            } else {
                child.setSize(
                        child.width(),
                        UIComponent.resolve(
                                child.verticalSizing(),
                                innerHeight,
                                child.contentHeightForLayout()));
            }
        }

        int cursor = vertical ? innerY : innerX;

        int mainExtent = vertical ? innerHeight : innerWidth;
        int slack = Math.max(0, mainExtent - this.totalMainLength(vertical));
        cursor +=
                vertical
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
                int offset =
                        switch (this.horizontalAlignment) {
                            case LEFT -> 0;
                            case CENTER -> (innerWidth - child.width()) / 2;
                            case RIGHT -> innerWidth - child.width();
                        };
                child.moveTo(innerX + offset, cursor);
                cursor += child.height() + child.margins().bottom() + this.gap;
            } else {
                int offset =
                        switch (this.verticalAlignment) {
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
