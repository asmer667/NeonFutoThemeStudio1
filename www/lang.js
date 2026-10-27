const translations = {
    ar: {
        title: "استوديو الثيمات",
        matchrules: "المطابقات",
        assets: "الأصول",
        colors: "الألوان",
        config: "الإعدادات",
        border_matchrules: "مطابقات الحدود",
        icon_assets: "أيقونات الأصول",
        other_assets: "أصول أخرى",
        export: "تصدير",
        import: "استيراد",
        preview: "معاينة",
        editor: "المحرر",
        home: "الرئيسية"
    },
    en: {
        title: "FUTO Theme Studio",
        matchrules: "Matchrules",
        assets: "Assets",
        colors: "Colors",
        config: "Config",
        border_matchrules: "BORDER MATCHRULES",
        icon_assets: "ICON ASSETS",
        other_assets: "OTHER ASSETS",
        export: "Export",
        import: "Import",
        preview: "Preview",
        editor: "Editor",
        home: "Home"
    }
};

let currentLang = 'ar';

function toggleLanguage() {
    currentLang = currentLang === 'ar' ? 'en' : 'ar';
    applyLanguage();
}

function applyLanguage() {
    const t = translations[currentLang];
    document.body.setAttribute('dir', currentLang === 'ar' ? 'rtl' : 'ltr');
    
    // تحديث النصوص
    document.querySelectorAll('[data-i18n]').forEach(el => {
        const key = el.getAttribute('data-i18n');
        if (t[key]) {
            el.innerText = t[key];
        }
    });
    
    // تحديث زر اللغة
    const langBtn = document.getElementById('lang-toggle');
    if (langBtn) langBtn.innerText = currentLang === 'ar' ? 'EN' : 'عربي';
}

// ربط أزرار الشريط السفلي بالوظائف الحقيقية
document.addEventListener('DOMContentLoaded', () => {
    // زر التصدير (Export)
    const exportBtn = document.querySelector('.bottom-nav .nav-item:nth-child(5)');
    if (exportBtn) {
        exportBtn.onclick = () => {
            // محاكاة الضغط على قائمة File -> Export
            const event = new KeyboardEvent('keydown', {
                key: 'e', ctrlKey: true, bubbles: true
            });
            document.dispatchEvent(event);
        };
    }

    // زر المعاينة (Preview) - يمكن ربطه لاحقاً
    const previewBtn = document.querySelector('.bottom-nav .nav-item:nth-child(3)');
    if (previewBtn) {
        previewBtn.onclick = () => {
            alert(currentLang === 'ar' ? 'المعاينة ستتوفر قريباً' : 'Preview coming soon');
        };
    }
});
