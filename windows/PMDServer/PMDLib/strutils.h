#pragma once

// @julenka:
// right now I am a bit too lazy to include the boost library
// but i should replace these by just using the boost library
// http://www.boost.org/doc/libs/1_52_0/more/getting_started/windows.html
// 


#include <algorithm> 
#include <functional> 
#include <cctype>
#include <locale>

#define SSTR( x ) ( dynamic_cast< std::ostringstream & >( ( std::ostringstream() << std::dec << x ) ) ).str() 

// from http://stackoverflow.com/questions/216823/whats-the-best-way-to-trim-stdstring

// trim from start
static inline std::string &ltrim(std::string &s) {
        s.erase(s.begin(), std::find_if(s.begin(), s.end(), std::not1(std::ptr_fun<int, int>(std::isspace))));
        return s;
}

// trim from end
static inline std::string &rtrim(std::string &s) {
        s.erase(std::find_if(s.rbegin(), s.rend(), std::not1(std::ptr_fun<int, int>(std::isspace))).base(), s.end());
        return s;
}

// trim from both ends
static inline std::string &trim(std::string &s) {
        return ltrim(rtrim(s));
}