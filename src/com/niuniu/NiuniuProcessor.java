package com.niuniu;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.niuniu.config.NiuniuBatchConfig;


/**
 * Servlet implementation class HelloServlet
 */
@WebServlet("/analyze")
public class NiuniuProcessor extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
    /**
     * @see HttpServlet#HttpServlet()
     */
    public NiuniuProcessor() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		response.getWriter().write(request.getParameter("message"));
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		request.setCharacterEncoding("UTF-8");
		response.setContentType("text/html;charset=UTF-8");
		String str = request.getParameter("message");
		String user_id = request.getParameter("user_id");
		String disable_cache = request.getParameter("disable_cache");
		ResourceMessageProcessor resourceMessageProcessor = new ResourceMessageProcessor();
		resourceMessageProcessor.setUserId(user_id);
		resourceMessageProcessor.setMessages(str);
		if(disable_cache!=null){
			resourceMessageProcessor.setDisableCache(Boolean.parseBoolean(disable_cache));
		}
		
		resourceMessageProcessor.process();
		response.getWriter().write(resourceMessageProcessor.resultToJson());
	}

}
