// دوال للتحكم في المحرر من تطبيق الأندرويد
window.FutoNativeBridge = {
    // فتح نافذة التصدير الحقيقية
    openExport: function() {
        // البحث عن زر Export في المحرر (حسب الكود الأصلي)
        const exportBtn = document.querySelector('.tabs button:first-child'); // تبويب المطابقات
        if(exportBtn) exportBtn.click();
        
        // محاكاة الضغط على File > Export
        setTimeout(() => {
            const menuFile = document.querySelector('menu-bar') || document.querySelector('.menu');
            if (menuFile) {
                // محاولة إيجاد زر التصدير
                const buttons = document.querySelectorAll('button');
                for (let btn of buttons) {
                    if (btn.innerText.includes('Export')) {
                        btn.click();
                        return;
                    }
                }
            }
            // إذا لم يجد الزر، يفتح نافذة التصدير مباشرة عبر DOM
            if (window.Ui && window.Ui.openWindow) {
                // استدعاء نافذة التصدير الأصلية
                const exportWindow = document.querySelector('[data-window="Export"]');
                if(exportWindow) exportWindow.style.display = 'block';
            }
        }, 300);
    },
    
    // فتح نافذة استيراد الأصول
    openImport: function() {
        const fileInput = document.querySelector('input[type="file"]');
        if (fileInput) {
            fileInput.click();
        } else {
            alert('يرجى فتح نافذة الأصول أولاً');
        }
    },

    // تغيير لغة الواجهة
    setLanguage: function(lang) {
        document.body.setAttribute('dir', lang === 'ar' ? 'rtl' : 'ltr');
        // هنا يتم استدعاء نظام الترجمة إذا كان موجوداً
        if (window.applyLanguage) {
            window.applyLanguage(lang);
        }
    }
};
