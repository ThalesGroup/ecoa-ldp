FROM ubuntu:noble
ENV LANG=en_US.utf8
ENV LANGUAGE=en_US.utf8
ENV LC_ALL=en_US.utf8
ENV TZ=Europe/Berlin
ENV SOFTARC_HOME=/ecoa/ldp
ENV SYS_BASHRC=/etc/bash.bashrc

WORKDIR /ecoa

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
  gpg \
  python3 \
  python3.12-venv \
  pip \
  unzip \
  locales \
  net-tools \
  tmux \
  neovim \
  wget \
  iputils-ping \
  openssh-client \
  ssh-tools \
  zsh \
  # ECOA TOOLS REQUIREMENTS \
  clang \
  # ant is installed locally from an archive \
  libapr1-dev \
  libcunit1 \
  liblog4cplus-dev \
  libxslt1-dev \
  libxml2-dev \
  cython3 \
  pkg-config \
  openjdk-11-jdk-headless \
  build-essential \
  autoconf \
  libtool \
  sudo \
  valgrind \
  && localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8 \
  && /usr/bin/apt-get remove -y openjdk-21-jre-headless

RUN ./create_libs.sh
