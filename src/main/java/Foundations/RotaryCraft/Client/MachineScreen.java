package Foundations.RotaryCraft.Client;

import Foundations.RotaryCraft.Gui.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Machine-specific fields over a shared compact inventory layout. No client-side machine mutation. */
public final class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    public MachineScreen(MachineMenu menu,Inventory inventory,Component title) {super(menu,inventory,title);imageWidth=248;imageHeight=238;inventoryLabelY=144;}
    private void action(int id){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Rotate"),b->action(0)).bounds(leftPos+180,topPos+24,60,20).build());
        addRenderableWidget(Button.builder(Component.literal("Gear mode"),b->action(1)).bounds(leftPos+180,topPos+46,60,20).build());
        addRenderableWidget(Button.builder(Component.literal("Range −"),b->action(2)).bounds(leftPos+180,topPos+72,60,20).build());
        addRenderableWidget(Button.builder(Component.literal("Range +"),b->action(3)).bounds(leftPos+180,topPos+94,60,20).build());
        addRenderableWidget(Button.builder(Component.literal("Analog"),b->action(4)).bounds(leftPos+180,topPos+116,60,20).build());
    }
    @Override public void render(GuiGraphics gui,int mouseX,int mouseY,float partial) {
        int i=0;for(var widget:renderables)if(widget instanceof Button button){button.visible=switch(i++){case 0->menu.value(12)>=0;case 1->menu.value(13)>=0;default->menu.value(8)>0;};}
        super.render(gui,mouseX,mouseY,partial);renderTooltip(gui,mouseX,mouseY);
    }
    @Override protected void renderBg(GuiGraphics gui,float partial,int mouseX,int mouseY) {
        int x=leftPos,y=topPos;
        gui.fill(x-1,y-1,x+imageWidth+1,y+imageHeight+1,0xff333e48);gui.fill(x,y,x+imageWidth,y+imageHeight,0xffd7d9dc);
        gui.fill(x,y,x+imageWidth,y+20,0xff34434d);
        gui.fill(x+176,y+20,x+imageWidth,y+imageHeight,0xffb8c0c5);
        for(var slot:menu.slots){int sx=x+slot.x,sy=y+slot.y;gui.fill(sx-1,sy-1,sx+17,sy+17,0xff626d75);gui.fill(sx,sy,sx+16,sy+16,0xffa8adb3);}
        int capacity=menu.value(1);if(capacity>0){gui.fill(x+8,y+38,x+170,y+43,0xff78858c);gui.fill(x+8,y+38,x+8+(int)(162L*Math.min(capacity,Math.max(0,menu.value(0)))/capacity),y+43,0xffddac3a);}
        int duration=menu.value(3);if(duration>0){gui.fill(x+8,y+69,x+170,y+74,0xff78858c);gui.fill(x+8,y+69,x+8+(int)(162L*Math.min(duration,Math.max(0,menu.value(2)))/duration),y+74,0xff71a785);}
    }
    @Override protected void renderLabels(GuiGraphics gui,int mouseX,int mouseY) {
        gui.drawString(font,font.plainSubstrByWidth(title.getString(),232),8,6,0xffffff,false);
        if(menu.value(1)>0)gui.drawString(font,compact(menu.value(0))+" / "+compact(menu.value(1))+" FE",8,25,0x26343c,false);
        else gui.drawString(font,menu.value(14)>=0?(menu.value(14)==1?"Switch enabled":"Disabled by redstone"):"Machine status",8,25,0x26343c,false);
        if(menu.value(4)>0||menu.value(5)>0)gui.drawString(font,compact(menu.value(4))+" rad/s · "+compact(menu.value(5))+" Nm",8,48,0x26343c,false);
        else if(menu.value(6)>0||menu.value(7)>0)gui.drawString(font,"Fluid: "+menu.value(6)+" mB",8,48,0x26343c,false);
        else if(menu.value(8)>0)gui.drawString(font,"Range "+menu.value(8)+" · output "+menu.value(10),8,48,0x26343c,false);
        else gui.drawString(font,menu.value(11)==1?"Active":"Idle",8,48,0x26343c,false);
        if(menu.value(3)>0)gui.drawString(font,"Progress "+menu.value(2)+" / "+menu.value(3),8,58,0x26343c,false);
        if(menu.sorter) {gui.drawString(font,"N",110,92,0x26343c,false);gui.drawString(font,"S",110,110,0x26343c,false);gui.drawString(font,"Down",110,128,0x26343c,false);}
        if(menu.machineSlots==0)gui.drawString(font,"Shift + item for hand use",8,88,0x26343c,false);
        if(menu.machineSlots>0)gui.drawString(font,menu.machineSlots==10?"Input / routing filters":"Machine inventory",8,78,0x26343c,false);
        gui.drawString(font,playerInventoryTitle,8,144,0x26343c,false);
        gui.drawString(font,"Shift-click",180,158,0x26343c,false);
        gui.drawString(font,"moves items",180,170,0x26343c,false);
    }
    private static String compact(int value){return value>=1_000_000?String.format(java.util.Locale.ROOT,"%.1fM",value/1_000_000.0):value>=10_000?String.format(java.util.Locale.ROOT,"%.1fk",value/1000.0):Integer.toString(value);}
}
