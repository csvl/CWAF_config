FROM rockylinux:9 as base

# RUN dnf -y update && dnf install -y git libtool g++ pcre-devel libxml2 yajl lmdb-libs
#
# RUN git clone https://github.com/owasp-modsecurity/ModSecurity.git && \
#     cd ModSecurity && \
#     git submodule init && \
#     git submodule update && \
#     ./build.sh && \
#     ./configure && \
#     make && \
#     make install

# ===================================================

# RUN yum install -y gcc-c++ flex bison yajl curl-devel zlib-devel pcre-devel git # yajl-devel GeoIP-devel doxygen curl  
#
# RUN cd /opt/ && \
#     git clone https://github.com/SpiderLabs/ModSecurity && \
#     cd ModSecurity && \
#     # git checkout -b v3/master origin/v3/master && \
#     sh build.sh && \
#     git submodule init && \
#     git submodule update && \
#     ./configure && \
#     yum install https://archives.fedoraproject.org/pub/archive/fedora/linux/updates/23/x86_64/b/bison-3.0.4-3.fc23.x86_64.rpm && \
#     make && \
#     make install

# ====================================================

# RUN dnf install -y httpd mod_security
#
# RUN systemctl enable httpd

# ====================================================

RUN dnf install -y sudo

COPY waf_inst /opt/waf_inst
WORKDIR /opt/waf_inst
RUN chmod a+x *.sh && ./install.sh
