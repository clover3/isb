from PIL import Image
origimg = Image.open('menubar_24.png')
origpixels = origimg.load()

newimg = Image.new('LA', (origimg.size[0],origimg.size[1]), (0,0))
newpixels = newimg.load() # create the pixel map


for i in range(origimg.size[0]):    # for every col:
    for j in range(origimg.size[1]):    # For every row
        #a = origimg.getpixel((i,j))
        #print a
        #newimg.setpixel((i,j),a)
        (l, a) = origpixels[i,j]
        newpixels[i,j] = (255-l, a)

newimg.save("menubar_white_24.png")
