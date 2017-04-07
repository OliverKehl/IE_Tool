package com.niuniu;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class HelloServlet
 */
@WebServlet("/HelloServlet")
public class HelloServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public HelloServlet() {
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
		ResourceMessageProcessor resourceMessageProcessor = new ResourceMessageProcessor();
		resourceMessageProcessor.setMessages(str);
		if(!resourceMessageProcessor.checkValidation()){
			System.out.println("不符合规范");
			return;
		}
		resourceMessageProcessor.process();
		ArrayList<String> base_car_ids = resourceMessageProcessor.res_base_car_ids;
		ArrayList<String> colors = resourceMessageProcessor.res_colors;
		ArrayList<String> discount_way = resourceMessageProcessor.res_discount_way;
		ArrayList<String> discount_content = resourceMessageProcessor.res_discount_content;
		ArrayList<String> remark = resourceMessageProcessor.res_remark;
		for(int i=0;i<base_car_ids.size();i++){
			if(base_car_ids.get(i).isEmpty())
				continue;
			response.getWriter().write(base_car_ids.get(i) + "\t" + colors.get(i) + "\t" + discount_way.get(i) + "\t" + discount_content.get(i) + "\t" + remark.get(i) + "\n");
		}
	}

}
