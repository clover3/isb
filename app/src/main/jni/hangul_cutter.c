#include <jni.h>
#include <stdio.h>
#include <string.h>
#include<android/log.h>
#define LOGI(msg) __android_log_print(ANDROID_LOG_INFO, "libnav", msg) 
 
/* hangleCountLiens.
 * Count the expected number of lines of a given comment line.
 * Usage: Refer com.postech.isb.readThread.ReadThread.commentWatcherInput.onTextChanged
 *
 * @param orig: First String class argument. UTF-8 String comment.
 * @return: Int[] for a tuble of two integer. First: The number of full lines. Second:  The number of remain characters.
 */
JNIEXPORT jintArray JNICALL Java_com_postech_isb_readThread_ReadThread_hangulCountLines(JNIEnv* env, jobject thiz, jstring jstr)
{
	const char *orig = (*env)->GetStringUTFChars(env, jstr, NULL);
	int len = strlen(orig);
	int ret[2] = {0,0};
	int i, j, char_byte = 0, char_num = 0;
	jintArray result;
	for (i = 0, j = 0; i < len && j < 50;){
		if((((int)orig[i]) & (int)0x80) == (int)0x00)
			i += 1, char_byte += 1;
		else if((((int)orig[i]) & (int)0xE0) == (int)0xC0)
			i += 2, char_byte += 2;
		else if((((int)orig[i]) & (int)0xF0) == (int)0xE0)
			i += 3, char_byte += 2;
		else
			i += 4, char_byte += 2;
		char_num += 1;
		if (char_byte == 50){
			ret[0] += 1;
			char_byte = 0;
		}
		else if (char_byte > 50){
			ret[0] += 1;
			char_byte = 2;
		}
	}
	ret[1] = char_byte;
	result = (*env)->NewIntArray(env, 2);
	if (result == NULL)
		return NULL;
	(*env)->SetIntArrayRegion(env, result, 0, 2, ret);
	return result;
}

/* hangleCutter.
 * Help leaving a many lines comment.
 * Because this applications is run in UTF-8, and isb uses CP949, there is a difference between counting the number of characters.
 * To make a line with the right number of characters(<50), this function's help is needed.
 * This function tells how many characters make one line.
 * Usage: Refer com.postech.isb.util.IsbSession.leaveCommnet
 * Note. One hangle character is counted as 1 in java String class.
 *
 * @param orig: First String class argument. UTF-8 String comment.
 * @return: Int[] which represents the number of characters which each line contains.
 */
JNIEXPORT jintArray JNICALL Java_com_postech_isb_util_IsbSession_hangulCutter(JNIEnv* env, jobject thiz, jstring jstr)
{
	const char *orig = (*env)->GetStringUTFChars(env, jstr, NULL);
	int len = strlen(orig);
	int cut_len[50] = {0,};
	int i, j, char_byte = 0, char_num = 0;
	jintArray result;
	for (i = 0, j = 0; i < len && j < 50;){

		if((((int)orig[i]) & (int)0x80) == (int)0x00)
			i += 1, char_byte += 1;
		else if((((int)orig[i]) & (int)0xE0) == (int)0xC0)
			i += 2, char_byte += 2;
		else if((((int)orig[i]) & (int)0xF0) == (int)0xE0)
			i += 3, char_byte += 2;
		else
			i += 4, char_byte += 2;
		char_num += 1;
		if (char_byte == 50){
			cut_len[j++] = char_num;
			char_byte = 0;
		}
		else if (char_byte > 50){
			cut_len[j++] = char_num-1;
			char_byte = 2;
		}
	}
	result = (*env)->NewIntArray(env, j);
	if (result == NULL)
		return NULL;
	(*env)->SetIntArrayRegion(env, result, 0, j, cut_len);
	return result;
}
JNIEXPORT jint JNICALL Java_com_postech_isb_util_IsbSession_hangulLength(JNIEnv* env, jobject thiz, jstring jstr)
{
	const char *orig = (*env)->GetStringUTFChars(env, jstr, NULL);
	int len = strlen(orig);
	return len;
}

JNIEXPORT jintArray JNICALL Java_com_postech_isb_util_IsbSession_hangulDebug(JNIEnv *env, jobject thiz, jstring jstr){
	const char *orig = (*env)->GetStringUTFChars(env, jstr, NULL);
	int len = strlen(orig);
	int cut_len[100] = {0,};
	int i, j, char_byte = 0, char_num = 0;
	jintArray result;
	char debug[100];
//	LOGI("hangulDebug");
	for (i = 0, j = 0; i < len && j < 100;){
		sprintf(debug, "%d, %d, %d", orig[i], ((int)orig[i]) & 0x80, (((int)orig[i]) & (int)0x80) == (int)0x00);
//		LOGI(debug);
		if((((int)orig[i]) & (int)0x80) == (int)0x00){
			i += 1, char_byte += 1, cut_len[j++] = 0;
		}
		else if((((int)orig[i]) & (int)0xE0) == (int)0xC0){
			i += 2, char_byte += 2, cut_len[j++] = 1;
		}
		else if((((int)orig[i]) & (int)0xF0) == (int)0xE0){
			i += 3, char_byte += 2, cut_len[j++] = 2;
		}
		else{
			i += 4, char_byte += 2, cut_len[j++] = 3;
		}
	}
	result = (*env)->NewIntArray(env, j);
	if (result == NULL)
		return NULL;
	(*env)->SetIntArrayRegion(env, result, 0, j, cut_len);
	return result;
}
JNIEXPORT jintArray JNICALL Java_com_postech_isb_util_IsbSession_hangulAscii(JNIEnv *env, jobject thiz, jstring jstr){
	const char *orig = (*env)->GetStringUTFChars(env, jstr, NULL);
	int len = strlen(orig);
	int cut_len[100] = {0,};
	int i, j=0;
	jintArray result;
	for (i = 0; i < 100 && i < len; i++){
		cut_len[j++] = (((int)orig[i])&0x80)==0x00;
	}
	result = (*env)->NewIntArray(env, j);
	if (result == NULL)
		return NULL;
	(*env)->SetIntArrayRegion(env, result, 0, j, cut_len);
	return result;
}
