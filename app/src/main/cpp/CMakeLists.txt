cmake_minimum_required(VERSION 3.22.1)

project("completelinuxinstaller_pty")

add_library(
    pty
    SHARED
    pty.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    pty
    ${log-lib}
)
