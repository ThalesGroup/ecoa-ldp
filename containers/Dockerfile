FROM ubuntu:noble
WORKDIR /ecoa
ENV LANG=en_US.utf8
ENV LANGUAGE=en_US.utf8
ENV LC_ALL=en_US.utf8
ENV TZ=Europe/Berlin
ENV SOFTARC_HOME=/ecoa/ldp
ENV SYS_BASHRC=/etc/bash.bashrc

COPY files/hostname /etc/hostname
COPY files/create_libs.sh .
RUN chmod +x create_libs.sh

RUN echo "export PATH=/ecoa/apache-ant-1.10.8/bin:$PATH:/usr/local/ldp/bin" > ${SYS_BASHRC}
RUN echo "export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:/usr/local/lib:/usr/local/ldp/lib" >> ${SYS_BASHRC}
RUN echo "export SOFTARC_HOME=/ecoa/ldp" >> ${SYS_BASHRC}
RUN echo "export WORK=/ecoa/WORK" >> ${SYS_BASHRC}
RUN echo "export PS1=\"EcoaAS7:\w >> \"" >> ${SYS_BASHRC} && chmod +x ${SYS_BASHRC}

RUN --mount=type=cache,target=/var/cache/apt \
	/usr/bin/apt-get update -qq \
	&& /usr/bin/apt-get install -y curl \
	  git \
	  wget \
	  gpg \
	  python3 \
	  python3.12-venv \
	  pip \
	  unzip

RUN curl -sL -o /zenoh-public-key https://download.eclipse.org/zenoh/debian-repo/zenoh-public-key
RUN gpg --dearmor --yes --output /etc/apt/keyrings/zenoh-public-key.gpg /zenoh-public-key
RUN cat <<EOF > /etc/apt/sources.list.d/zenoh.sources
Types: deb
URIs: https://download.eclipse.org/zenoh/debian-repo/
Suites: /
Signed-By: /etc/apt/keyrings/zenoh-public-key.gpg
EOF
RUN --mount=type=cache,target=/var/cache/apt \
	/usr/bin/apt-get update -qq \
	&& /usr/bin/apt-get install -y locales \
		net-tools \
		tmux \
		neovim \
		wget \
		iputils-ping \
		openssh-client \
		ssh-tools \
		zsh \
		# ECOA TOOLS REQUIREMENTS \
		maven \
		clang \
		# ant is installed locally from an archive \
		libapr1-dev \
		libcunit1 \
		liblog4cplus-dev \
		libxslt1-dev \
		libxml2-dev \
		cython3 \
		pkg-config \
		cmake \
		openjdk-11-jdk-headless \
		nlohmann-json3-dev \
		build-essential \
		autoconf \
		libtool \
		sudo \
		valgrind \
    libzenohc=1.6.2 \
		libzenohc-dev=1.6.2 \
		libzenohcpp-dev=1.6.2 \
		zenoh=1.6.2 \
		zenohd=1.6.2 \
		zenoh-plugin-rest=1.6.2 \
		zenoh-plugin-remote-api=1.6.2 \
                zenoh-plugin-storage-manager=1.6.2 \
	&& localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8 \
	&& /usr/bin/apt-get remove -y openjdk-21-jre-headless

RUN ./create_libs.sh
