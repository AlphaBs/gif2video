# gif2video
android app for converting gif to video

Compression test

| Original | Option1 | Ratio |
| -------- | -------- | ----- |
| [t1.gif](https://github.com/AlphaBs/gif2video/blob/master/test_video/t1.gif) 7,290kb | [t1.mp4](https://github.com/AlphaBs/gif2video/blob/master/test_video/t1.mp4) 341kb | 95% |
| [t2.gif](https://github.com/AlphaBs/gif2video/blob/master/test_video/t2.gif) 7,411kb | [t2.mp4](https://github.com/AlphaBs/gif2video/blob/master/test_video/t1.mp4) 919kb | 87% |
| [t3.gif](https://github.com/AlphaBs/gif2video/blob/master/test_video/t3.gif) 9,966kb | [t3.mp4](https://github.com/AlphaBs/gif2video/blob/master/test_video/t3.mp4) 784kb | 92% |

Option1 : libx264 default   
`-c:v libx264 -movflags faststart -vf "pad=ceil(iw/2)*2:ceil(ih/2)*2" -pix_fmt yuv480p`
