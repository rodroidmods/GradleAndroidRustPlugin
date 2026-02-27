#ifndef RUSTIOS_H
#define RUSTIOS_H

#include <stdint.h>

char* rust_greeting_c(const char* name);
void rust_greeting_free(char* s);
uint64_t fibonacci_c(uint32_t n);

#endif
