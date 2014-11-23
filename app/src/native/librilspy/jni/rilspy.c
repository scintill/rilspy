/*
 * Copyright (c) 2014 Joey Hewitt <joey@joeyhewitt.com>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 *
 *
 *  Based on smsdispatch.c from:
 *  Collin's Dynamic Dalvik Instrumentation Toolkit for Android
 *  Collin Mulliner <collin[at]mulliner.org>
 *
 *  (c) 2012,2013
 *
 *  License: LGPL v2.1
 *
 */

#include <android/log.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <errno.h>
#include <stdbool.h>
#include <unistd.h>

#include "base.h" // adbi base
#include "hook.h"

#define ALOGD(...) __android_log_print(ANDROID_LOG_DEBUG, "librilspy", __VA_ARGS__)
static void my_log(char *msg) { ALOGD("%s", msg); }

bool getfdname(int fd, char *buf, size_t bufsize) {
	char tbuf[256];

	snprintf(tbuf, sizeof(tbuf), "/proc/self/fd/%d", fd);
	ssize_t s = readlink(tbuf, buf, bufsize);
	if (s == -1) {
		//ALOGD("readlink(%s) error: %s", tbuf, strerror(errno));
		return false;
	}
	return true;
}

void dump_io(const char *iotype, const char *preposition, int fd, const void *buf, size_t count) {
	char filename[256];
	char msgStr[4096];
	if (getfdname(fd, filename, count)) {
		snprintf(msgStr, sizeof(msgStr), "%s %d bytes %s file %s: ", iotype, count, preposition, filename);
	} else {
		snprintf(msgStr, sizeof(msgStr), "%s %d bytes %s fd %d: ", iotype, count, preposition, fd);
	}

	for (size_t i = 0; i < count; i++) {
		// XXX this is inefficient, to repeatedly iterate through all existing chars
		snprintf(msgStr, sizeof(msgStr), "%s%c", msgStr, ((char *)buf)[i]);
	}
	ALOGD("%s", msgStr);
}

static struct hook_t hook_write;
static ssize_t my_write(int fd, const void *buf, size_t count) {
	dump_io("write", "to", fd, buf, count);

	ssize_t (*orig_write)(int fd, const void *buf, size_t count) = (void*)hook_write.orig;
	hook_precall(&hook_write);
	ssize_t res = orig_write(fd, buf, count);
	hook_postcall(&hook_write);

	return res;
}

static struct hook_t hook_read;
static ssize_t my_read(int fd, const void *buf, size_t count) {

	ssize_t (*orig_read)(int fd, const void *buf, size_t count) = (void*)hook_read.orig;
	hook_precall(&hook_read);
	ssize_t res = orig_read(fd, buf, count);
	hook_postcall(&hook_read);

	if (res == -1) {
		ALOGD("read(%d, void*, %d) failed: %s", fd, count, strerror(errno));
	} else {
		dump_io("read", "from", fd, buf, res);
	}

	return res;
}

// set my_init as the entry point
void __attribute__ ((constructor)) static my_init(void);

static void my_init(void) {
	ALOGD("initializing");

	// set log function for  libbase (very important!)
	set_logfunction(my_log);

	hook(&hook_write, getpid(), "libc.", "write", my_write, 0);
	hook(&hook_read, getpid(), "libc.", "read", my_read, 0);
}
