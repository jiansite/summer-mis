package cn.cerc.jui.parts;

import cn.cerc.jpage.core.Component;

public class UISheet extends UIComponent {
    private String caption = "(无标题)";
    private String group = "工具面板";

    @Deprecated
    public UISheet() {
        super(null);
    }

    public UISheet(Component owner) {
        super(owner);
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}