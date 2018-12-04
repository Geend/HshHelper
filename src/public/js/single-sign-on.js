$(document).ready(function() {
    $(".loginForm").each(function () {
        var form = $(this);

        $(this).submit(function(e){
            var credentialId = $(this).attr("x-credential-id");

            $.ajax({
                dataType: "json",
                url: "/credentials/"+credentialId,
                async: false,
                success: function(data) {
                    form.find(".username").each(function () {
                        $(this).val(data["username"]);
                    });

                    form.find(".password").each(function () {
                        $(this).val(data["password"])
                    });
                }
            });
        });
    });
});