# This Fix is necessary because of a misconfiguration in remapbody where they check the wrong module containing the commands they want to use
sed -i "s/substitute_module/filter_module/g" httpd_rules/common/macros/remapbody.conf
