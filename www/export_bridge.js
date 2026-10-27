// دالة ربط تصدير الـ ZIP مع الأندرويد
window.futoExportTheme = function() {
    // محاكاة فتح قائمة File -> Export في المحرر
    // هذا يعتمد على هيكل المحرر الداخلي
    const exportBtn = document.querySelector('button[aria-label="Export"], .export-btn');
    if (exportBtn) {
        exportBtn.click();
    } else {
        alert('الرجاء التصدير من قائمة File > Export داخل المحرر');
    }
};
