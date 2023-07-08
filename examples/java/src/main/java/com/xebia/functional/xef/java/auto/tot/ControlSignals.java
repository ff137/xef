package com.xebia.functional.xef.java.auto.tot;

import java.util.concurrent.CompletableFuture;

import static com.xebia.functional.xef.java.auto.tot.Rendering.renderHistory;
import static com.xebia.functional.xef.java.auto.tot.Rendering.truncateText;

public class ControlSignals {

    static class ControlSignal {
        String value;
    }

    public static <A> CompletableFuture<ControlSignal> controlSignal(Problems.Memory<A> memory){
        System.out.println("\uD83E\uDDE0 Generating control signal for problem:" + truncateText(memory.problem.description) + "...");
        String guidancePrompt = Rendering.trimMargin(
                "    You are an expert advisor on information extraction.\n" +
                "    You generate guidance for a problem.\n" +
                "    " + renderHistory(memory) + "\n" +
                "    You are given the following problem:\n" +
                "    " + memory.problem.description + "\n" +
                "    Instructions:\n" +
                "    1. Generate 1 guidance to get the best results for this problem.\n" +
                "    2. Ensure the guidance is relevant to the problem.\n" +
                "    3. Ensure the guidance is accurate, complete, and unambiguous.\n" +
                "    4. Ensure the guidance is actionable.\n" +
                "    5. Ensure the guidance accounts for previous answers in the `history`.\n" +
                "    \n");

            return memory.getAiScope().prompt(guidancePrompt, ControlSignal.class);
    }

}
