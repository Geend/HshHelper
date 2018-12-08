$(document).ready(function() {
    $(".loginForm").each(function () {
        var form = $(this);

        $(this).submit(function(e){
            var csrfField = $("#csrf_container").find("input");
            var csrfToken = csrfField.val();
            var csrfTokenFieldName = csrfField.attr("name");

            var credentialId = $(this).attr("x-credential-id");

            var postData = {};
            postData[csrfTokenFieldName] = csrfToken;
            postData["credentialId"] = credentialId;

            $.ajax({
                dataType: "json",
                url: "/credentials/decrypt",
                async: false,
                method: "POST",
                data: postData,
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