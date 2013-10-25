
/*
 * test2.c
 *
 *  Created on: Mar 9, 2012
 *      Author: dvi
 */
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libswscale/swscale.h>
#include <SDL.h>
#include <stdio.h>

void sdl_init(AVFormatContext* format_context, AVCodecContext*
              codec_context, int videostream ) {
    SDL_Init(SDL_INIT_EVERYTHING);
    AVFrame* frame;
    AVFrame *pFrameRGB;
    AVPacket avpacket;
    AVPicture  picture;


    // Convert the image from its native format to RGB
    struct SwsContext *img_convert_ctx =NULL;

    SDL_Event event;
    SDL_RendererInfo info;
    SDL_Texture *texture;
    SDL_Surface *image;
    Uint32 then, now , frames;
    int w = 640;
    int h = 480;
    int done = 0;
    SDL_Window * window = SDL_CreateWindow("SDL",
                                           SDL_WINDOWPOS_UNDEFINED, SDL_WINDOWPOS_UNDEFINED, w, h, 0);
    SDL_Renderer * renderer = SDL_CreateRenderer(window, -1, 0);

    SDL_GetRendererInfo(renderer, &info);
    printf("Using %s rendering\n", info.name);



    SDL_Log("+++++ INIT DONE +++++");


    frame = avcodec_alloc_frame();

    pFrameRGB = avcodec_alloc_frame();
    if (pFrameRGB == NULL)
    {
        printf("Cannot allocate pFrame\n");
        exit(-1);
    }

    unsigned char* pixels;
    int pitch;
    uint8_t *buffer;
    int numBytes;
    // Determine required buffer size and allocate buffer
    numBytes=avpicture_get_size(PIX_FMT_RGB24, codec_context->width,
                                codec_context->height);
    buffer=(uint8_t *)av_malloc(numBytes*sizeof(uint8_t));


    // Assign appropriate parts of buffer to image planes in pFrameRGB
    // Note that pFrameRGB is an AVFrame, but AVFrame is a superset
    // of AVPicture
    avpicture_fill((AVPicture *)pFrameRGB, buffer, PIX_FMT_RGB24,
                   codec_context->width, codec_context->height);

    while (av_read_frame(format_context, &avpacket) >= 0) {
        if (avpacket.stream_index == videostream) {
            // Video stream packet
            int frame_finished;

            avcodec_decode_video2(codec_context, frame, &frame_finished,
                                  &avpacket);

            if(frame_finished)
            {

                picture.data[0] = frame->data[0];
                picture.data[1] = frame->data[1];
                picture.data[2] = frame->data[2];
                picture.linesize[0] = frame->linesize[0];
                picture.linesize[1] = frame->linesize[1];
                picture.linesize[2] = frame->linesize[2];



                img_convert_ctx  = sws_getCachedContext(img_convert_ctx,
                                                        codec_context->width,
                                                        codec_context->height,
                                                        //PIX_FMT_RGB24,
                                                        PIX_FMT_YUV420P,
                                                        frame->width,
                                                        frame->height,
                                                        PIX_FMT_RGB24,
                                                        SWS_BICUBIC,
                                                        NULL,
                                                        NULL,
                                                        NULL);


                if (img_convert_ctx == NULL)
                {
                    fprintf(stderr, "Cannot initialize the conversion context!\n");
                    exit(1);
                }



                int s = sws_scale(
                                  img_convert_ctx,
                                  frame->data,
                                  frame->linesize,
                                  0,
                                  codec_context->height,
                                  picture.data,
                                  picture.linesize);


                texture = SDL_CreateTexture(renderer, SDL_PIXELFORMAT_YV12,
                                            SDL_TEXTUREACCESS_STREAMING, codec_context->width,
                                            codec_context->height);//3 plane texture

                if (!texture) {
                    fprintf(stderr, "Couldn't set create texture: %s\n",
                            SDL_GetError());
                    exit(-1);
                }


                if(texture && !SDL_LockTexture(texture, NULL, (void **)&pixels,
                                               &pitch) ) {

                    if(pitch == picture.linesize[0]) {
                        int size = pitch * frame->height;
                        memcpy(pixels, picture.data[0], size);
                        memcpy(pixels + size, picture.data[2], size / 4);
                        memcpy(pixels + size * 5 / 4, picture.data[1], size / 4);
                    }


                    else {
                        register unsigned char *y1,*y2,*y3,*i1,*i2,*i3;
                        int i;
                        y1 = pixels;
                        y3 = pixels + pitch * frame->height;
                        y2 = pixels + pitch * frame->height * 5 / 4;

                        i1=picture.data[0];
                        i2=picture.data[1];
                        i3=picture.data[2];

                        for (i = 0; i<(frame->height/2); i++) {
                            memcpy(y1,i1,pitch);
                            i1+=picture.linesize[0];
                            y1+=pitch;
                            memcpy(y1,i1,pitch);

                            memcpy(y2,i2,pitch / 2);
                            memcpy(y3,i3,pitch / 2);

                            y1+=pitch;
                            y2+=pitch / 2;
                            y3+=pitch / 2;
                            i1+=picture.linesize[0];
                            i2+=picture.linesize[1];
                            i3+=picture.linesize[2];
                        }
                    }
                }//texture
                SDL_UnlockTexture(texture);
            }//frame
            av_free_packet(&avpacket);

        }//avpacket

        //while (!done) {
        SDL_PollEvent(&event);

        switch(event.type) {

            if (event.key.keysym.sym == SDLK_ESCAPE) {
                done = 1;
            }
            break;

            case SDL_QUIT:
            //done=1;
            SDL_Log("Unhandled event type=%d", event.type);
            done=1;
            break;
        }
        /* Print out some timing information */
        //  printf("TIMING %d\n",avpacket.dts);
        //SDL_UpdateTexture(texture, NULL, imageYUV, image->w)
        if (!SDL_UpdateTexture(texture, NULL, frame, frame->width)) {
            //SDL_UpdateTexture(texture, NULL, imageYUV, image->w);

            fprintf(stderr, "Couldn't  update texture: %s\n",
                    SDL_GetError());
            //exit(-1);
        }

        SDL_RenderClear(renderer);
        SDL_RenderCopy(renderer, texture, NULL, NULL);
        SDL_RenderPresent(renderer);
        SDL_Delay(20);
        //}

        //        		}

}//while av_read_frame

SDL_Log("+++++ FINISHED +++++");
SDL_Quit();
}


int main(int argc, char * argv[]) {

    AVCodecContext* codec_context;
    int videostream;

    if (argc < 2) {
        printf("Usage: %s filename\n", argv[0]);
        return 0;
    }

    // Register all available file formats and codecs
    av_register_all();

    int err;
    // Init SDL with video support
    err = SDL_Init(SDL_INIT_VIDEO);
    if (err < 0) {
        fprintf(stderr, "Unable to init SDL: %s\n", SDL_GetError());
        return -1;
    }

    // Open video file
    const char* filename = argv[1];
    AVFormatContext* format_context = NULL;
    err = avformat_open_input(&format_context, filename, NULL, NULL);
    if (err < 0) {
        fprintf(stderr, "ffmpeg: Unable to open input\n");
        return -1;
    }

    // Retrieve stream information
    err = avformat_find_stream_info(format_context, NULL);
    if (err < 0) {
        fprintf(stderr, "ffmpeg: Unable to find stream info\n");
        return -1;
    }

    // Dump information about file onto standard error
    av_dump_format(format_context, 0, argv[1], 0);

    // Find the first video stream

    for (videostream = 0; videostream < format_context->nb_streams;
         ++videostream) {
        if (format_context->streams[videostream]->codec->codec_type ==
            AVMEDIA_TYPE_VIDEO) {
            break;
        }
    }
    if (videostream == format_context->nb_streams) {
        fprintf(stderr, "ffmpeg: Unable to find video stream\n");
        return -1;
    }

    codec_context = format_context->streams[videostream]->codec;
    AVCodec* codec = avcodec_find_decoder(codec_context->codec_id);

    avcodec_alloc_context3(codec);


    if (avcodec_open2(codec_context, codec, NULL) < 0)
    {
        fprintf(stderr, "ffmpeg: Unable to allocate codec context\n");
    }

    else {
        printf("Codec initialized\n");

    }

    /*
       Initializing display
     */
    printf("Width:%d\n",codec_context->width);
    printf("height:%d\n",codec_context->height);
    //exit(0);


    sdl_init(format_context, codec_context,videostream);


    return 0;
}
