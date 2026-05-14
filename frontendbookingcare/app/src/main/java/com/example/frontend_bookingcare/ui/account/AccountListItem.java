package com.example.frontend_bookingcare.ui.account;

import androidx.annotation.NonNull;

public class AccountListItem {

    public enum Kind {
        SECTION, ROW, FOOTER
    }

    @NonNull
    public final Kind kind;

    public String sectionTitle;
    public String rowId;
    public String title;
    public String subtitle;
    public String value;
    public String emoji;
    /** 0: hiển thị emoji; khác 0: drawable trong ô icon */
    public int iconRes;
    public int iconBgRes;

    private AccountListItem(@NonNull Kind kind) {
        this.kind = kind;
    }

    public static AccountListItem section(String title) {
        AccountListItem i = new AccountListItem(Kind.SECTION);
        i.sectionTitle = title;
        return i;
    }

    public static AccountListItem row(String id, String title, String subtitle, String value, String emoji, int iconBgRes) {
        AccountListItem i = new AccountListItem(Kind.ROW);
        i.rowId = id;
        i.title = title;
        i.subtitle = subtitle;
        i.value = value;
        i.emoji = emoji;
        i.iconRes = 0;
        i.iconBgRes = iconBgRes;
        return i;
    }

    /** Dòng menu với icon vector (tùy màn Điều khoản, v.v.) */
    public static AccountListItem rowWithVectorIcon(String id, String title, String subtitle, String value, int iconRes, int iconBgRes) {
        AccountListItem i = new AccountListItem(Kind.ROW);
        i.rowId = id;
        i.title = title;
        i.subtitle = subtitle;
        i.value = value;
        i.emoji = null;
        i.iconRes = iconRes;
        i.iconBgRes = iconBgRes;
        return i;
    }

    public static AccountListItem footer() {
        return new AccountListItem(Kind.FOOTER);
    }
}
