import io
from PIL import Image, ImageDraw
def addLines(x):
    bytez = bytes(x)
    im = Image.open(io.BytesIO(bytez))
    draw = ImageDraw.Draw(im)
    draw.line((0, 0) + im.size, width=3)
    imgByteArr = io.BytesIO()
    im.save(imgByteArr, format='PNG')
    imgByteArr = imgByteArr.getvalue()
    return imgByteArr

def onlyLines(x):
    bytez = bytes(x)
    im = Image.open(io.BytesIO(bytez))
    im = im.convert("RGBA")
    datas = im.getdata()
    newData = [(255, 255, 255, 0) for i in datas]
    im.putdata(newData)
    draw = ImageDraw.Draw(im)
    draw.line((0, 0) + im.size, width=3)
    imgByteArr = io.BytesIO()
    im.save(imgByteArr, format='PNG')
    imgByteArr = imgByteArr.getvalue()
    return imgByteArr