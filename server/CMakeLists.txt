cmake_minimum_required(VERSION 3.26)
project(Mousedroid)

set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_BINARY_DIR}/bin)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED TRUE)

if(MSVC)
    add_compile_options(/Zc:preprocessor)
endif()

if(WIN32)
    # On Windows, vcpkg handles wxWidgets and Asio automatically.
    # find_package(asio CONFIG) is the vcpkg standard for Asio.
    find_package(asio CONFIG REQUIRED)
    find_package(wxWidgets REQUIRED net gl core base adv)
    
    # Define Windows-specific libraries (added user32 for SendInput)
    set(PLATFORM_LIBS ws2_32 wsock32 user32)
    
    # MSVC vs MinGW resource handling
    if(MSVC)
        set(PLATFORM_SRC "src/input/win32/windowsinputmanager.cpp" resource.rc)
    else()
        set(PLATFORM_SRC "src/input/win32/windowsinputmanager.cpp" resource.o)
        set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wl,-subsystem,windows")
    endif()
else()
    # Linux: Use system-installed wxWidgets
    find_package(wxWidgets REQUIRED net gl core base adv)
    set(PLATFORM_SRC "src/input/linux/linuxinputmanager.cpp")
    set(PLATFORM_LIBS "")
    
    # Manual ASIO path for Linux
    set(ASIO_INCLUDE_DIR "${ASIO_PATH}/include") 
endif()

# Collect all sources but avoid double-including input managers
file(GLOB_RECURSE ALL_SRC src/*.cpp)

if(WIN32)
    set(PLATFORM_SRC "src/input/win32/windowsinputmanager.cpp")
else()
    set(PLATFORM_SRC "src/input/linux/linuxinputmanager.cpp")
    set(PLATFORM_LIBS "")
endif()

list(REMOVE_ITEM ALL_SRC ${PLATFORM_SRC})

add_executable(Mousedroid WIN32 ${ALL_SRC} ${PLATFORM_SRC})



if(wxWidgets_FOUND)
    include(${wxWidgets_USE_FILE})
    target_link_libraries(Mousedroid PRIVATE ${wxWidgets_LIBRARIES})
endif()

target_link_libraries(Mousedroid PRIVATE ${PLATFORM_LIBS})

if(WIN32)
    # vcpkg-installed asio provides an imported target
    target_link_libraries(Mousedroid PRIVATE asio::asio)
else()
    target_include_directories(Mousedroid PRIVATE ./src ${ASIO_INCLUDE_DIR})
endif()

target_include_directories(Mousedroid PRIVATE ./src)

# ===========================================================================
# Test infrastructure
# ===========================================================================
enable_testing()

include(FetchContent)

FetchContent_Declare(
    googletest
    GIT_REPOSITORY https://github.com/google/googletest.git
    GIT_TAG        v1.14.0
)
# For Windows: Prevent overriding the parent project's compiler/linker settings
set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)

# Disable installing GoogleTest
set(INSTALL_GTEST OFF CACHE BOOL "" FORCE)

FetchContent_MakeAvailable(googletest)

# Test executable — tests only the protocol/dispatch logic that doesn't
# require wxWidgets, Asio networking, or OS-specific input injection.
add_executable(pocket_pad_tests
    tests/test_protocol_and_dispatch.cpp
)

# The test binary needs access to the inputmanager.hpp header for opcode
# constants and the Base class definition. We provide only the header-level
# include path, not the full src/ tree, to avoid pulling in GUI/networking
# dependencies.
target_include_directories(pocket_pad_tests PRIVATE
    ${CMAKE_SOURCE_DIR}/src
    ${googletest_SOURCE_DIR}/googletest/include
)

target_link_libraries(pocket_pad_tests PRIVATE
    GTest::gtest
)

# Register the test with CTest
include(GoogleTest)
gtest_discover_tests(pocket_pad_tests)
