package com.mycompany.apbapp.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;

@Named
@SessionScoped
public class GuestPreferences implements Serializable {

    private String theme = "blue";

    //private String layout = "light";
    private String layout = "blue";

    private String menuMode = "menu-layout-static";

    private String inputStyle = "outlined";

    private String menuColor = "dark";

    private List<Theme> themes;

    private List<Layout> layouts;

    @PostConstruct
    public void init() {
        themes = new ArrayList<>();
        themes.add(new Theme("Blue", "blue", "#2196F3"));
        themes.add(new Theme("Green", "green", "#4CAF50"));
        themes.add(new Theme("Orange", "orange", "#FFC107"));
        themes.add(new Theme("Purple", "purple", "#9C27B0"));

        layouts = new ArrayList<>();
        layouts.add(new Layout("Blue", "blue", "#0388e5"));
        layouts.add(new Layout("Light", "light", "#ffffff"));
        layouts.add(new Layout("Dark", "dark", "#4d5058"));
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public String getMenuMode() {
        return menuMode;
    }

    public void setMenuMode(String menuMode) {
        this.menuMode = menuMode;
    }

    public String getInputStyleClass() {
        return this.inputStyle.equals("filled") ? "ui-input-filled" : "";
    }

    public String getInputStyle() {
        return inputStyle;
    }

    public void setInputStyle(String inputStyle) {
        this.inputStyle = inputStyle;
    }

    public String getMenuColor() {
        return menuColor;
    }

    public void setMenuColor(String menuColor) {
        this.menuColor = menuColor;
    }

    public List<Theme> getThemes() {
        return themes;
    }

    public void setThemes(List<Theme> themes) {
        this.themes = themes;
    }

    public List<Layout> getLayouts() {
        return layouts;
    }

    public void setLayouts(List<Layout> layouts) {
        this.layouts = layouts;
    }

    public class Theme {

        private String name;

        private String file;

        private String color;

        public Theme() {
        }

        public Theme(String name, String file, String color) {
            this.name = name;
            this.file = file;
            this.color = color;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }

    public class Layout {

        private String name;

        private String file;

        private String color;

        public Layout() {
        }

        public Layout(String name, String file, String color) {
            this.name = name;
            this.file = file;
            this.color = color;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }
}
