$(function () {
    $('.demo1').colorpicker();
    $('.color-pic').colorpicker();
    
    // Horizontal mode
        $('#demo_forceformat').colorpicker({
            format: 'rgba', // force this format
            horizontal: true
        });
    // Horizontal mode
        $('#demo_forceformat3').colorpicker({
            format: 'rgba', // force this format
        });
    
    
        $('.demo-auto').colorpicker();
});
