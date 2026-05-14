export const clinicTheme = {
  token: {
    colorPrimary: '#2563EB',
    colorInfo: '#2563EB',
    colorSuccess: '#16A34A',
    colorWarning: '#F59E0B',
    colorError: '#DC2626',
    colorTextBase: '#0f172a',
    colorBgBase: '#f5f7fb',
    colorBgContainer: '#ffffff',
    borderRadius: 14,
    fontFamily: `system-ui, -apple-system, "Segoe UI", Roboto, Arial, sans-serif`,
  },
  components: {
    Layout: {
      bodyBg: '#f5f7fb',
      headerBg: '#ffffff',
      siderBg: '#ffffff',
    },
    Menu: {
      itemBorderRadius: 10,
      itemMarginInline: 8,
      itemMarginBlock: 4,
      itemHeight: 44,
      activeBarBorderWidth: 0,
    },
    Card: {
      borderRadiusLG: 14,
    },
    Table: {
      borderRadiusLG: 14,
      headerBg: '#f8fafc',
      headerColor: '#0f172a',
    },
    Button: {
      borderRadius: 10,
    },
    Modal: {
      borderRadiusLG: 16,
    },
    Breadcrumb: {
      lastItemColor: '#0f172a',
      linkColor: 'rgba(15,23,42,0.68)',
    },
  },
} as const

