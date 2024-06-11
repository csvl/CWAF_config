#!/bin/bash

# ------------------------------------------------------------------------------
# Choose your options (usually yes/no)
# If you don't (empty), user will be prompted during install.
# Better to copy the exports in a separate shell that calls this one. 

# Stop/start httpd (better if possible)
#export waf_inst_opt_stop=yes

# Update only existing setup (ignore all settings hereafter, ignored if first install)
#export waf_inst_opt_update=yes

# Managed Service: machine id (port number for reverse SSH)
# Use "-" if no managed service (or to not modify the existing one)
#export waf_inst_machine_id=-

# Install Elastic agent
#export waf_inst_opt_elastic=yes

# Disable logs rotation
#export waf_inst_opt_nologrotate=no

# Disable monthly reboot
#export waf_inst_opt_noreboot=no

# Copy common config (if newer)
#export waf_inst_opt_conf_common=yes

# Copy template config
#export waf_inst_opt_conf_default=yes

# Copy error pages
#export waf_inst_opt_htdocs=yes

# Directory containing additional files/dir to install:
# GeoIP.conf, *.user, htdocs, common, company, machine, site, ssl
#export waf_inst_extra_dir=/root

# In case both options below are set to "no", no change is performed to automatic updates
# Enable automatic updates (unless you use updated images)
#export waf_harden_opt_autoupdate=yes
# Disable automatic updates (reverts previous setting)
#export waf_harden_opt_noautoupdate=no

# Overwrite anacrontab file (always done during first install, even if set to 'no')
#export waf_inst_opt_anacron=no

# Disable ipv6 (unless you need it)
#export waf_harden_opt_ipv6d=yes

# Disable sshd root login (dangerous)
# In case Approach operators accounts are not created correctly,
# no way to connect to the machine except from the hypervisor
#export waf_harden_opt_sshd=no

# Timezone: default=Europe/Brussels
#export waf_inst_opt_tz=Europe/Brussels

# NTP server ("-" to not add an NTP server)
#export waf_inst_opt_ntp=-

# Install ACME support (for LetsEncypt, ...)
#export waf_inst_opt_acme=no

# Install geoipupdate (default is yes)
#export waf_inst_geoipupdate=no

# ------------------------------------------------------------------------------
# In case no DNS is defined (will be reset by NetworkManager)
echo "nameserver 8.8.8.8" >>  /etc/resolv.conf

# Detect OS version
grep -qs -m1 'PLATFORM_ID="platform:el[89]"' /etc/os-release || (
 echo '*** Platform not supported:'
 cat /etc/os-release
 exit 1
)
OsVer=$(cat /etc/os-release|gawk 'BEGIN {FS="\x22"}; /VERSION_ID/ {print $2}')
OsVerMaj=$(echo $OsVer|gawk 'BEGIN {FS="."}; {print $1}')
OsVerMin=$(echo $OsVer|gawk 'BEGIN {FS="."}; {print $2}')

echo "Installing loose dependencies ..."
# Enable some repos (cannot be done inside the RPM)
#  - EPEL for mod_auth_openidc & mod_maxminddb
dnf -q info epel-release | grep -qs 'Installed Packages' &> /dev/null || (
 epel=https://dl.fedoraproject.org/pub/epel/epel-release-latest-${OsVerMaj}.noarch.rpm
 dnf -yq install $epel || exit 1
)
/usr/bin/crb status | grep -q disabled && { /usr/bin/crb enable || exit 1; } # Required for EPEL

# Cannot be done inside the RPM without dependancy ('Recommends' is broken in dnf v4)
dnf -yq install bind-utils bzip2 chrony firewalld net-tools policycoreutils-python-utils psmisc socat sysstat tar || exit 1
# Needs a separate command
dnf -yq install dnf-automatic || exit 1

export ErrorFile=/tmp/approach-waf-ai.log
rm -f $ErrorFile &> /dev/null

version=$(dnf -q info approach-waf-ai 2> /dev/null | gawk -e '/^Source/ { sub(/^.*approach-waf-ai-/, ""); sub(/[.]src[.]rpm$/, ""); print; }')
if [[ "$version" == "" ]]; then
 echo "Installing WAF Application Intelligence ..."
 dnf -yq install    approach-waf-ai-*.rpm || exit $?
else
 if [[ -e approach-waf-ai-$version.x86_64.rpm ]]; then
  echo "Re-installing WAF Application Intelligence ..."
  dnf -yq reinstall approach-waf-ai-*.rpm || exit $?
 else
  echo "Updating WAF Application Intelligence ..."
  dnf -yq update    approach-waf-ai-*.rpm || exit $?
 fi
fi

# Done after as it may require a script installed by RPM
if [[ "$waf_inst_geoipupdate" != "no" ]]; then
 dnf -q info geoipupdate &> /dev/null
 if [[ $? != 0 ]]; then
  echo "Installing geoipupdate ..."
  if [[ "$OsVerMaj" == "8" ]]; then
   # In EPEL8
   dnf -yq install geoipupdate || exit $?
  fi
  if [[ "$OsVerMaj" == "9" ]]; then
  # dnf -y install epel-next epel-testing # For geoipupdate, not in EPEL9
   if [[ -e geoipupdate*.rpm ]]; then
    dnf -yq install geoipupdate*.rpm || exit $?
   else
    chmod u+x /usr/local/bin/geoipupdate.sh
    /usr/local/bin/geoipupdate.sh || exit $?
   fi
  fi
 fi
fi

# Remove useless packages (Alma Linux)
#>/dev/nul dnf -C info gnome* || dnf -yq remove libX*
#dnf -yq remove cockpit*

if [[ -e $ErrorFile ]]; then
 echo ""
 echo "----------------------------------------------"
 cat $ErrorFile
 rm -f $ErrorFile
fi
