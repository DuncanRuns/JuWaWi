package xyz.duncanruns.juwawi.command;

import org.apache.logging.log4j.Level;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.command.Command;
import xyz.duncanruns.juwawi.JuWaWiPlugin;

import java.util.Arrays;
import java.util.stream.Collectors;

public class SetPreviewPercentsCommand extends Command {

    @Override
    public String helpDescription() {
        return "setjuwawipp [1st preview %] [2nd preview %] [3rd preview %] ... - Sets the preview percentages JuWaWi will redraw instances on";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 100;
    }

    @Override
    public String getName() {
        return "setjuwawipp";
    }

    @Override
    public void run(String[] args, CancelRequester cancelRequester) {
        JuWaWiPlugin.options.updatePercents = Arrays.stream(args).map(Byte::parseByte).collect(Collectors.toList());
        Julti.log(Level.INFO, "(JuWaWi) Updated preview percentages for instance redrawing");
    }
}
